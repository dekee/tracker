## ArgoCD on Kind: build, deploy, operate, troubleshoot

This repo is deployed to a **local Kind cluster** using **ArgoCD** and **Kustomize**.

### Prereqs

- `docker`
- `kind`
- `kubectl`
- ArgoCD installed in your Kind cluster (namespace: `argocd`)

### 1) Build the app image

From repo root:

```bash
docker build -t tax-tracker:dev .
```

### 2) Load the image into Kind

First, check your Kind cluster name:

```bash
kind get clusters
```

If your cluster is **not** named `kind` (example: `argocd-lab`), you must pass `--name`:

```bash
kind load docker-image --name argocd-lab tax-tracker:dev
```

### 3) Create (or update) the ArgoCD Application

Apply:

```bash
kubectl -n argocd apply -f argocd/application-tax-tracker.yaml
```

Force ArgoCD to refresh/sync immediately:

```bash
kubectl -n argocd annotate application tax-tracker argocd.argoproj.io/refresh=hard --overwrite
```

ArgoCD syncs `k8s/overlays/kind` into namespace `tax-tracker`.

### 4) Configure secrets (Pushover + optional URL)

The deployment reads env vars from an **optional** secret:

- namespace: `tax-tracker`
- name: `tax-tracker-scrape-secret`

Create/update it like:

```bash
kubectl -n tax-tracker delete secret tax-tracker-scrape-secret --ignore-not-found

kubectl -n tax-tracker create secret generic tax-tracker-scrape-secret \
  --from-literal=RENTLY_URL="https://secure.rently.com/platform/activity_log?property=REPLACE_ME" \
  --from-literal=NOTIFY_PUSHOVER_ENABLED="true" \
  --from-literal=NOTIFY_PUSHOVER_TOKEN="REPLACE_WITH_PUSHOVER_APP_TOKEN" \
  --from-literal=NOTIFY_PUSHOVER_USER="REPLACE_WITH_PUSHOVER_USER_KEY"
```

Restart the app to pick up changes:

```bash
kubectl -n tax-tracker rollout restart deploy/tax-tracker
kubectl -n tax-tracker rollout status deploy/tax-tracker --timeout=180s
```

### 5) Access the app

```bash
kubectl -n tax-tracker port-forward svc/tax-tracker 8085:80
```

- UI: `http://localhost:8085`
- Trigger scrape/run: `http://localhost:8085/api/run-now`

### 6) Verify it’s working

**Pods are Running/Ready:**

```bash
kubectl -n tax-tracker get pods -o wide
```

**Database connectivity is OK** when app logs include:

- `HikariPool-1 - Start completed.`

```bash
kubectl -n tax-tracker logs -l app=tax-tracker --tail=200
kubectl -n tax-tracker logs -l app=tax-tracker-postgres --tail=200
```

**Check DB tables/counters:**

```bash
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "\\dt"

kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "SELECT 'parcel' t, count(*) FROM parcel UNION ALL SELECT 'status_change', count(*) FROM status_change;"
```

### 7) Troubleshooting (what we did)

#### A) `kind load docker-image ...` says “no nodes found for cluster kind”

Your cluster isn’t named `kind`. Use:

```bash
kind get clusters
kind load docker-image --name <cluster-name> tax-tracker:dev
```

#### B) `/api/run-now` returns instantly / no scraping happens

This is usually because there are **0 parcels** in DB, often due to missing `parcels.txt`.

- The app seeds parcels from a file mounted in-cluster:
  - `PARCELS_FILE=/app/parcels.txt`
  - Kustomize generates ConfigMap `tax-tracker-parcels` from `k8s/base/parcels.txt`

Confirm:

```bash
kubectl -n tax-tracker get configmap tax-tracker-parcels
kubectl -n tax-tracker logs -l app=tax-tracker --tail=200 | grep -E "runDaily processing|parcels file not found"
```

#### C) ArgoCD looks stuck on an old commit

Force a refresh:

```bash
kubectl -n argocd annotate application tax-tracker argocd.argoproj.io/refresh=hard --overwrite
kubectl -n argocd get application tax-tracker -o jsonpath='{.status.sync.status} {.status.health.status} {.status.sync.revision}{"\n"}'
```

#### D) “I changed the code but behavior didn’t change”

ArgoCD syncs manifests, but it does **not** rebuild images.

Rebuild + reload + restart:

```bash
docker build -t tax-tracker:dev .
kind load docker-image --name <cluster-name> tax-tracker:dev
kubectl -n tax-tracker rollout restart deploy/tax-tracker
```

#### E) Truncate DB (clean slate)

```bash
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "TRUNCATE TABLE parcel, status_change, tax_history RESTART IDENTITY CASCADE;"
```

#### F) Test Pushover notifications

Notifications only fire on **status changes**. To force a single test notification:

```bash
# reset status_change so the result is obvious
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "TRUNCATE TABLE status_change RESTART IDENTITY;"

# pick one parcel
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "SELECT parcel_id FROM parcel ORDER BY parcel_id LIMIT 1;"

# force it to a fake status
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "UPDATE parcel SET status='FORCE_NOTIFY_TEST' WHERE parcel_id='<parcel_id_from_above>';"

# trigger
kubectl -n tax-tracker port-forward svc/tax-tracker 8085:80
curl -sS http://localhost:8085/api/run-now

# verify
kubectl -n tax-tracker exec deploy/tax-tracker-postgres -- \
  psql -U postgres -d tasktracker -c "SELECT count(*) FROM status_change;"

kubectl -n tax-tracker logs -l app=tax-tracker --since=10m | grep -i pushover
```



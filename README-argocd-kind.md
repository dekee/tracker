## ArgoCD on Kind: deploy this app

### 1) Build the app image and load into Kind

From repo root:

```bash
./gradlew --no-daemon clean bootJar
docker build -t tax-tracker:dev .
kind load docker-image tax-tracker:dev
```

### 2) Create the ArgoCD Application

Edit `argocd/application-tax-tracker.yaml` and set `spec.source.repoURL` to your Git repo URL.

Then apply it:

```bash
kubectl apply -f argocd/application-tax-tracker.yaml
```

ArgoCD will sync `k8s/overlays/kind` into the `tax-tracker` namespace (it will create the namespace).

### 3) Access the app

Port-forward the Service:

```bash
kubectl -n tax-tracker port-forward svc/tax-tracker 8085:80
```

Then open `http://localhost:8085`.



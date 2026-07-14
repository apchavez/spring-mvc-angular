# Terraform — EKS Cluster

Provisions the AWS infrastructure this project's Helm chart (`../chart/`) deploys onto: a VPC, an EKS cluster with a managed node group, the EBS CSI driver (so `postgres.yaml`/`kafka.yaml`/`redis.yaml`'s PVCs can bind), and the `ingress-nginx` controller (`chart/templates/ingress.yaml` hardcodes `ingressClassName: nginx`).

> **This is not wired into CI.** `deploy.yml` assumes a cluster already exists and points `KUBECONFIG` at it — it does not run this Terraform. Provisioning/destroying real AWS infrastructure is a deliberate, manual, cost-incurring step, not something that should happen automatically on a push.

---

## ⚠️ Cost warning

Running `terraform apply` here creates **real, billed AWS resources**: an EKS control plane (~$0.10/hr), 1–3 `t3.medium` nodes (~$0.04/hr each), one NAT gateway (~$0.045/hr + data), an internet-facing Network Load Balancer for `ingress-nginx`, and EBS gp3 volumes for `postgres`/`kafka`/`redis`. Roughly **$150–200/month** if left running continuously. Unlike the AWS Lambda and Azure Functions siblings in this portfolio (serverless, effectively zero cost at rest), this is always-on infrastructure. **Always `terraform destroy` when you're done evaluating it.**

---

## Prerequisites

- [Terraform](https://developer.hashicorp.com/terraform/install) ≥ 1.5.7
- AWS credentials with permission to create VPCs, EKS clusters, IAM roles, and EC2 instances
- `kubectl` and `helm` (to deploy `../chart/` afterwards)

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # adjust region/size if needed
terraform init
terraform plan
terraform apply
```

Once applied, point `kubectl`/`helm` at the new cluster and deploy the chart:

```bash
$(terraform output -raw configure_kubectl)
helm upgrade --install product-service ../chart --namespace product-service-mvc --create-namespace
```

Tear everything down:

```bash
terraform destroy
```

## What this does *not* do

- Does not build or push Docker images — that's `ci.yml`'s `docker-api`/`docker-web` jobs.
- Does not configure DNS for `product-service.local` — either edit `/etc/hosts` to point at the `ingress-nginx` load balancer's address (`terraform output ingress_nginx_load_balancer_hint`) or swap in a real domain.
- Does not manage the GitHub Actions `KUBECONFIG`/`production` environment secret used by `deploy.yml` — that's a manual step (`aws eks update-kubeconfig` output, base64-encoded) if you want CI to deploy onto this cluster.

## Files

| File | Purpose |
|---|---|
| `main.tf` | VPC (single NAT gateway) + EKS cluster + managed node group |
| `addons.tf` | EBS CSI driver IAM role (Pod Identity), default `gp3` StorageClass, `ingress-nginx` Helm release |
| `providers.tf` | `aws`/`kubernetes`/`helm` provider wiring, using the EKS cluster's own auth token |
| `variables.tf` / `outputs.tf` | Inputs and outputs — see `terraform.tfvars.example` |

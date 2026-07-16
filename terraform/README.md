# Terraform — Cluster EKS

Provisiona la infraestructura de AWS sobre la que despliega el chart de Helm de este proyecto (`../chart/`): una VPC, un cluster EKS con un managed node group, el driver CSI de EBS (para que los PVC de `postgres.yaml`/`kafka.yaml`/`redis.yaml` puedan enlazar), y el controlador `ingress-nginx` (`chart/templates/ingress.yaml` tiene hardcodeado `ingressClassName: nginx`).

> **Esto no está conectado al CI.** `deploy.yml` asume que ya existe un cluster y apunta `KUBECONFIG` hacia él — no ejecuta este Terraform. Provisionar/destruir infraestructura real de AWS es un paso deliberado, manual y con costo, no algo que deba ocurrir automáticamente en cada push.

---

## ⚠️ Advertencia de costo

Correr `terraform apply` aquí crea **recursos reales de AWS con costo**: un control plane de EKS (~$0.10/hr), de 1 a 3 nodos `t3.medium` (~$0.04/hr cada uno), un NAT gateway (~$0.045/hr + datos), un Network Load Balancer expuesto a internet para `ingress-nginx`, y volúmenes EBS gp3 para `postgres`/`kafka`/`redis`. Aproximadamente **$150–200/mes** si se deja corriendo continuamente. A diferencia de los hermanos AWS Lambda y Azure Functions de este portafolio (serverless, costo prácticamente cero en reposo), esta es infraestructura siempre activa. **Siempre correr `terraform destroy` al terminar de evaluarlo.**

---

## Prerrequisitos

- [Terraform](https://developer.hashicorp.com/terraform/install) ≥ 1.5.7
- Credenciales de AWS con permisos para crear VPCs, clusters EKS, roles IAM e instancias EC2
- `kubectl` y `helm` (para desplegar `../chart/` después)

## Uso

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # ajustar región/tamaño si hace falta
terraform init
terraform plan
terraform apply
```

Una vez aplicado, apuntar `kubectl`/`helm` al nuevo cluster y desplegar el chart:

```bash
$(terraform output -raw configure_kubectl)
helm upgrade --install product-service ../chart --namespace product-service-mvc --create-namespace
```

Para destruir todo:

```bash
terraform destroy
```

## Lo que esto *no* hace

- No compila ni publica imágenes Docker — eso lo hacen los jobs `docker-api`/`docker-web` de `ci.yml`.
- No configura DNS para `product-service.local` — hay que editar `/etc/hosts` para apuntar a la dirección del load balancer de `ingress-nginx` (`terraform output ingress_nginx_load_balancer_hint`), o usar un dominio real.
- No gestiona el secreto `KUBECONFIG`/ambiente `production` de GitHub Actions que usa `deploy.yml` — eso es un paso manual (salida de `aws eks update-kubeconfig`, codificada en base64) si se quiere que el CI despliegue sobre este cluster.

## Archivos

| Archivo | Propósito |
|---|---|
| `main.tf` | VPC (un solo NAT gateway) + cluster EKS + managed node group |
| `addons.tf` | Rol IAM del driver CSI de EBS (Pod Identity), `StorageClass` `gp3` por defecto, release de Helm de `ingress-nginx` |
| `providers.tf` | Configuración de los providers `aws`/`kubernetes`/`helm`, usando el token de autenticación propio del cluster EKS |
| `variables.tf` / `outputs.tf` | Entradas y salidas — ver `terraform.tfvars.example` |

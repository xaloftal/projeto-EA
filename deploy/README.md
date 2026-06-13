# 🚀 CatchIt - Infraestrutura Cloud & Orquestração com GKE e Ansible

Este repositório contém a infraestrutura como código (IaC) e as rotinas de automação para o deployment do sistema distribuído **CatchIt** no **Google Kubernetes Engine (GKE)**, utilizando **Ansible Roles** e **Jinja2 Templates**.

---

## 🏗️ Arquitetura de Microserviços no Cluster

A aplicação foi desenhada seguindo um padrão de arquitetura distribuída, isolando as responsabilidades em múltiplos Pods dentro do ecossistema Kubernetes:

* **Camada de Entrada (Edge/Frontend)**: `catchit-frontend` (Exposto publicamente via `LoadBalancer` da GCP na porta `9000`).
* **Camada de Lógica de Negócio**: `catchit-backend` (Porta `8080`) & `catchit-payment` (Porta `8081`).
* **Camada de Persistência & Cache**: `catchit-db` (PostgreSQL com volumes persistentes GKE CSI) & `catchit-redis` (Cache interna em RAM para o serviço de notificações).

---

## 🛠️ Requisitos e Componentes do Sistema

Para gerir o cluster a partir do terminal local (WSL2/Ubuntu), foram instalados os seguintes componentes nucleares da **Google Cloud CLI**:
* **Google Cloud CLI Core Libraries**: Utilitários base de comandos `gcloud`.
* **gke-gcloud-auth-plugin (v0.5.10)**: Componente de segurança obrigatório para autenticação criptográfica entre o `kubectl` local e o plano de controlo do GKE.

---

## 🚀 Guia de Operação da Pipeline Local

### 1. Preparação e Envio das Imagens (Docker Hub)
Antes de disparar o Ansible, as imagens geradas localmente pelo Docker Desktop devem ser vinculadas ao repositório cloud oficial (`rodrigues17`):

```bash
# 1. Efetuar o login na consola local
docker login -u rodrigues17

# 2. Atribuir as tags remotas às imagens locais do projeto
docker tag projeto-ea-backend:latest rodrigues17/catchit-backend:latest
docker tag projeto-ea-payment-service:latest rodrigues17/catchit-payment:latest
docker tag projeto-ea-frontend:latest rodrigues17/catchit-frontend:latest

# 3. Upload das imagens para a cloud
docker push rodrigues17/catchit-backend:latest
docker push rodrigues17/catchit-payment:latest
docker push rodrigues17/catchit-frontend:latest

```

### 2. Ativação do Ambiente de Autenticação no Terminal
Para garantir que o kubectl reconhece o plugin da Google Cloud em cada sessão do Bash, execute:
```bash
echo 'export USE_GKE_GCLOUD_AUTH_PLUGIN=True' >> ~/.bashrc
source ~/.bashrc
```

### 3. Execução do Deployment via Ansible
Para processar os templates Jinja2 em memória, injetar os segredos encriptados e aplicar os manifestos no GKE, execute o Playbook principal:

# Contornando o bloqueio de permissões de ficheiros executáveis do Windows/WSL2
cp .vault_pass /tmp/.vault_pass
chmod 600 /tmp/.vault_pass

# Execução estável com o ficheiro de password limpo
ansible-playbook -i inventory/gcp.yml catchit-deploy.yml --vault-password-file /tmp/.vault_pass




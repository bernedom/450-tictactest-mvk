# Infrastructure (OpenTofu)

Provisions a single EC2 instance (Amazon Linux 2023, `t3.micro`) in the default VPC,
with a security group that allows SSH. Intended for an AWS Academy Learner Lab.

## Prerequisites

- [OpenTofu](https://opentofu.org/docs/intro/install/) >= 1.8. The repo's Nix flake
  provides it: run `direnv allow` once in the repo root (or use `nix develop`).
- A running Learner Lab session

## Credentials

Learner Lab credentials are temporary (they expire when the lab session ends).
In the lab, open **AWS Details → AWS CLI: Show** and paste the block into
`~/.aws/credentials`:

```ini
[default]
aws_access_key_id=...
aws_secret_access_key=...
aws_session_token=...
```

Repeat this every time you start a new lab session.

For SSH, download `labsuser.pem` from **AWS Details → Download PEM**, then:

```bash
mv ~/Downloads/labsuser.pem ~/.ssh/ && chmod 400 ~/.ssh/labsuser.pem
```

## Usage

```bash
cd infra
tofu init
tofu plan
tofu apply
# ...
tofu destroy
```

Override defaults with `-var`, e.g. restrict SSH to your own IP:

```bash
tofu apply -var "ssh_cidr=$(curl -s https://checkip.amazonaws.com)/32"
```

## Notes

- State is stored locally in `terraform.tfstate` (git-ignored). Don't delete it while
  resources exist, or tofu loses track of them.
- Learner Lab stops instances when the session ends, but doesn't delete them.
  Run `tofu destroy` when you're done to save budget.

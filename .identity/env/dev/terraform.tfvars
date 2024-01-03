prefix    = "pagopa"
env       = "dev"
env_short = "d"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "pagoPA"
  Source      = "https://github.com/pagopa/pagopa-stand-in-manager"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

cd_github_federations = [
  {
    repository = "pagopa-stand-in-manager"
    subject    = "dev"
  }
]

environment_cd_roles = {
  subscription = [
    "Contributor",
    "Storage Account Contributor",
    "Storage Blob Data Contributor",
    "Storage File Data SMB Share Contributor",
    "Storage Queue Data Contributor",
    "Storage Table Data Contributor",
    "Key Vault Contributor"
  ]
#  subscription = [
#    "Contributor",
#    "Storage Account Contributor",
#    "Storage Blob Data Contributor",
#    "Storage File Data SMB Share Contributor",
#    "Storage Queue Data Contributor",
#    "Storage Table Data Contributor",
#    "Key Vault Contributor"
#  ]
#  resource_groups = {}
  resource_groups = {
    "pagopa-d-nodo-sec-rg" = [
      "Key Vault Contributor"
    ],
    "pagopa-d-weu-dev-aks-rg" = [
      "Contributor"
    ]
  }
}
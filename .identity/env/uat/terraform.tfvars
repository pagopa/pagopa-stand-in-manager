prefix    = "pagopa"
env       = "uat"
env_short = "u"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "pagoPA"
  Source      = "https://github.com/pagopa/pagopa-stand-in-manager"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

cd_github_federations = [
  {
    repository = "pagopa-stand-in-manager"
    subject    = "uat"
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
 resource_groups = {
   "pagopa-u-nodo-sec-rg" = [
     "Key Vault Contributor"
   ],
   "pagopa-u-weu-uat-aks-rg" = [
     "Contributor"
   ]
 }
}
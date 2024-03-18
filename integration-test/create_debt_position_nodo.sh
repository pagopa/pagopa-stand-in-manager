#!/usr/bin/env bash

env=$1
subscription_key=$2
organization="77777777777"
prefix="17"
suffix="999"
random_digits=""
for i in {1..12}; do
    random_digits+=$((RANDOM % 10))
done
iuv="${prefix}${random_digits}${suffix}"

due_date=$(date -u -v+7d +"%Y-%m-%dT%H:%M:%S.%3Z")

data="{
         \"organizationFiscalCode\": \"${organization}\",
         \"fee\": 0,
         \"status\": \"PO_UNPAID\",
         \"iupd\": \"STANDIN-${iuv}\",
         \"type\": \"F\",
         \"fiscalCode\": \"KWLTKL22V55P301T\",
         \"fullName\": \"Mario Rossi\",
         \"streetName\": \"via martini\",
         \"civicNumber\": \"19 bc1\",
         \"postalCode\": \"00042\",
         \"city\": \"Roma\",
         \"province\": \"Roma\",
         \"country\": \"IT\",
         \"email\": \"email@provider.com\",
         \"companyName\": \"Comune di Castel\",
         \"validityDate\": null,
         \"switchToExpired\": \"false\",
         \"paymentOption\": [
             {
                 \"iuv\": \"${iuv}\",
                 \"amount\": 2000,
                 \"description\": \"pagamento test stand-in\",
                 \"isPartialPayment\": true,
                 \"dueDate\": \"$due_date\",
                 \"fee\": 0,
                 \"transfer\": [
                     {
                         \"organizationFiscalCode\": \"${organization}\",
                         \"idTransfer\": \"1\",
                         \"amount\": 2000,
                         \"remittanceInformation\": \"pagamento test stand-in\",
                         \"category\": \"9/1923202TS/\",
                         \"iban\": \"IT30N0103076271000001823603\"
                     }
                 ]
             }
         ]
     }"


curl --location "https://api.$env.platform.pagopa.it/gpd/debt-positions-service/v1/organizations/$organization/debtpositions?toPublish=true" \
--header "Ocp-Apim-Subscription-Key: $subscription_key" \
--header "Content-Type: application/json" \
--data-raw "${data}"

echo "\nPay the following debt position:"
echo "- EC: ${organization}"
echo "- NAV: 3${iuv}"
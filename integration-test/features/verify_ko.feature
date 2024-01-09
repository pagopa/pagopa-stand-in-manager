Feature: Verify ko of payments
#  Execute a verifyPaymentNotice to not-responding station

  Background:
    Given systems up

  @runnable
  Scenario: Send verifyPaymentNotice
    Given an unique FdR name named flow_name
    When PSP sends create request to fdr-microservice with payload
    Then PSP receives the HTTP status code 201 to create request

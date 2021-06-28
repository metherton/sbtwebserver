Feature: Keepalive
  Scenario: Keepalive
    Given keepalive is called
    Then response should be 200
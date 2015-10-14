Feature: Basic Authentication
  Scenario: The user does not provide authentication
    Given the user does not provide authentication:
    When he requests application
    Then he gets a 401 status code
  Scenario: The user enters bad password
    Given the user provides bad authentication:
    When he requests application
    Then he gets a 401 status code
  Scenario: The user enters good password
    Given the user provides good authentication:
    When he requests application
    Then he gets a 200 status code

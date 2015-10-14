Feature: Catalog
  Scenario: The user forgets the header
    Given the user does not provides the header:
    When he requests the catalog
    Then he gets a 412 status code
  Scenario: The asks for a bad version
    Given the user asks for version X.X
    When he requests the catalog
    Then he gets a 412 status code
  Scenario: The asks for the good version
    Given the user asks for version 2.6
    When he requests the catalog
    Then he gets a 200 status code
    And he gets the good catalog

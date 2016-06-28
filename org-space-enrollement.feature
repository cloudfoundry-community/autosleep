Feature: org and space auto enrollment

  Scenario: initial space enrollment

    Given a CF instance with the following org, space
      | org           | private domains          | shared domains      | spaces             |
      | system_domain | cf.paas-provider.com     | cfapps-provider.com | autosleep,admin-ui |
      | team-a-prod   | portal.acme.com          | cfapps-provider.com | portal             |
      | team-a-dev    | dev.acme.com             | cfapps-provider.com | portal             |
      | team-b        | dev.bar.com,prod.bar.com | cfapps-provider.com | dev, prod          |
      | sandbox       |                          | cfapps-provider.com | usera,userb,userc  |
    And the following autosleep service instances in each space
      | org           | space     | autosleep service instances |
      | system_domain | autosleep |                             |
      | system_domain | admin-ui  |                             |
      | system_domain | admin-ui  |                             |

    When autosleep is deployed with the following configuration flags
      """
      # the username of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.username=
      #the password of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.password=

      #one of: disabled, standard (opt-out possible), forced (only transient opt-out)
      enrollspace.mode=true
      # Periodicity of the space enrollment
      enrollspace.perodicity=T10M

      # Indicates whether the CC API user has cloudcontroller.admin permissions. If not, it should be org admin of each of the orgs to enroll
      enrollspace.cf.client.is-cc-admin=false

      # Include or exclude orgs and space from enrollment using regular expressions
      enrollspace.include-orgs=sandbox
      enrollspace.exclude-orgs=*prod
      enrollspace.include-space=*
      enrollspace.exclude-space=prod*
      enrollspace.exclude-space=prod*
      #Exclude shared from autowakeup registration
      enrollspace.exclude-domain=portal.acme.com


      #Specify per space configuration

      #Specify autosleep config in enrolled spaces
      #Override default 24H idle duration
      enrollspace.space-config.idle-duration=T10H
      #Choose apps to exclude from enrollment
      enrollspace.space-config.exclude-from-auto-enrollment=
      #app enrollment mode among: standard, forced
      enrollspace.space-config.auto-enrollment=standard
      #enrollspace.space-config.secret=Th1s1zg00dP@$$w0rd

      """

    Then autosleep periodically automatically scans orgs and spaces (excluding ones matching a reg exp)
    And an autosleep service instance is created in each space
    And the autosleep-assigned CC API user is added as a space developer to act on apps there
    And for each shared domain, a wildcard route is created and bound to the autowakeup application

  Scenario: permanent opt out from space enrollment (mode=standard)
  Scenario: transient opt out from space enrollment (mode=forced)
  Scenario: self service override of idle duration or exclusion ?
  Scenario: handling of apps bound to private domains: no autowake up
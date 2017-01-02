Feature: private paas service provider org and space autoenrollment

  A private service provider wishes to optimize resources within its company while preserving flexibility and control
  for service consummers (team productivity remains a priority w.r.t. to resource optimization).

  Service consummers app teams (org admins and space developers) may be given capability to disable autosleep
  or configure autosleep (e.g. exclude apps, tune idle duration) in a self service mode.
  They select the standard space enrollement mode in which opt-outs are permanent.

  In some cases, the service provider may normalize naming of the orgs and spaces, and use this naming conventions to
  automatically assign autosleep configuration based on the names (e.g. prod vs non-prod), with a default configuration,
  and org and space exclusion patterns.

  Applications autowakeup is applied on all shared domains provided by the service providers (with the preq that shared
  domains wildcard route get mapped to the autosleep application). Application autowakeup on private domains would need
  service consummers/service providers actions to route orphan traffic to the autosleep public route.

  Background:
    Given autosleep is deployed by service provider with the following configuration flags
      """
      # the username of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.username=
      #the password of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.password=

      #one of: disabled, standard (opt-out possible), forced (only transient opt-out)
      enrollspace.mode=standard
      # Periodicity of the space enrollment
      enrollspace.service.instance.name=autosleep-autoenrolled
      # Periodicity of the space enrollment (and transient opt-out)
      enrollspace.perodicity=T1D

      # Indicates whether the CC API user has cloudcontroller.admin permissions. If not, it should be org admin of each of the orgs to enroll
      enrollspace.cf.client.is-cc-admin=true

      # Include or exclude orgs and space from enrollment using regular expressions
      enrollspace.include-orgs=*
      enrollspace.exclude-orgs=(*prod|system_domain)
      enrollspace.include-space=*
      enrollspace.exclude-space=prod*
      #Exclude shared from autowakeup registration
      enrollspace.exclude-domain=portal.acme.com


      #Specify autosleep service-instance default configuration (as arbitrary params) applied on every auto enrolled space,
      # or to opt-ins when no arbitrary params are set

      #Specify autosleep config in enrolled spaces
      #Override default 24H idle duration
      default.idle-duration=T10H
      #Choose apps to exclude from enrollment
      default.exclude-from-auto-enrollment=
      #app enrollment mode among: standard, forced (standard applies when unspecified)
      default.auto-enrollment=standard
      #default.secret=Th1s1zg00dP@$$w0rd

      """


  Scenario: new org and space discovered triggers default config autoenrollment except excluded spaces

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org           | spaces             |
      | system_domain | autosleep,admin-ui |
      | team-a-prod   | portal             |
      | team-a-dev    | portal             |
      | team-b        | dev, prod          |
      | sandbox       | usera,userb,userc  |
    And the autosleep service instances in each space are
      | org           | space     | autosleep service instances (arbitrary params) |
      | system_domain | autosleep |                                                |
      | system_domain | admin-ui  |                                                |
      | team-a-prod   | portal    |                                                |
      | team-a-dev    | portal    |                                                |
      | team-b        | dev       |                                                |
      | team-b        | prod      |                                                |
      | sandbox       | usera     |                                                |
      | sandbox       | userb     |                                                |
      | sandbox       | userc     |                                                |

    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org           | space     | autosleep service instances (arbitrary params)                       |
      | system_domain | autosleep |                                                                      |
      | system_domain | admin-ui  |                                                                      |
      | team-a-prod   | portal    |                                                                      |
      | team-a-dev    | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-b        | dev       | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-b        | prod      |                                                                      |
      | sandbox       | usera     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | sandbox       | userb     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | sandbox       | userc     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |

  Scenario: permanent opt out from space enrollment (enrollspace.mode=standard)

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    When the autosleep-autoenrolled service instance is deleted by service consummer
    Then the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params) |
      | team-a-dev | portal |                                                |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params) |
      | team-a-dev | portal |                                                |


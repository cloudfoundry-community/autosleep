Feature: public paas org and space autoenrollmennt

  A public service provider offers CF self service mode, where customers create new orgs and spaces (choosing how to name them), and may delete spaces, and orgs.

  The service provider would like to have autosleep automatically applied with default configuration on all new orgs and spaces created.

  In addition, upon commercial negociations with customers, the service provider needs to tune the autosleep
  configuration on a customer per customer basis (e.g. exclude orgs, spaces, apps, or tune idle duration).

  The autosleep service (default service plan) is made visible to all orgs in the market place. This is useful for customers
  to access documentation associated with autosleep service instances that appear in their space and understand the actions
  performed on their applications.

  Service consummers are not given capability to disable autosleep or configure autosleep
  (e.g. exclude apps, tune idle duration) in a self service mode.

  While service consummer may create new autosleep service instances in their space, the service-provider-instanciated
  service instance still applies. Service-consummer-instanciated autosleep service instance would in effect only reduce
  idle duration of enrolled apps.

  Applications autowakeup is applied on all shared domains provided by the service providers (with the preq that shared
  domains wildcard route get mapped to the autosleep application). Application autowakeup on private domains would need
  service consummers actions to route orphan traffic to the autosleep public route.

  Orgs and spaces are being applied default configuration.

  Background:
    Given autosleep is deployed by service provider with the following configuration flags
      """
      # the username of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.username=
      #the password of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.password=

      #one of: disabled, standard (opt-out possible), forced (only transient opt-out)
      enrollspace.mode=forced
      # Periodicity of the space enrollment
      enrollspace.service.instance.name=autosleep-autoenrolled
      # Periodicity of the space enrollment (and transient opt-out)
      enrollspace.perodicity=T10M

      # Indicates whether the CC API user has cloudcontroller.admin permissions. If not, it should be org admin of each of the orgs to enroll
      enrollspace.cf.client.is-cc-admin=true

      # Include or exclude orgs and space from enrollment using regular expressions
      enrollspace.include-orgs=*
      enrollspace.exclude-orgs=(*prod|system_domain)
      enrollspace.include-space=*
      enrollspace.exclude-space=prod*
      #Exclude shared from autowakeup registration
      enrollspace.exclude-domain=portal.acme.com


      #Specify autosleep service-instance configuration (as arbitrary params) applied on every auto enrolled space

      #Specify autosleep config in enrolled spaces
      #Override default 24H idle duration
      enrollspace.space-config.idle-duration=T10H
      #Choose apps to exclude from enrollment
      enrollspace.space-config.exclude-from-auto-enrollment=
      #app enrollment mode among: standard, forced
      enrollspace.space-config.auto-enrollment=standard
      #enrollspace.space-config.secret=Th1s1zg00dP@$$w0rd

      """


  Scenario: new org and space discovered triggers default config autoenrollment

    Given a CF instance with the following orgs, spaces
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
      | system_domain | admin-ui  |                                                                      |
      | team-a-prod   | portal    |                                                                      |
      | team-a-dev    | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-b        | dev       | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-b        | prod      |                                                                      |
      | sandbox       | usera     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | sandbox       | userb     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | sandbox       | userc     | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |


  Scenario: no org and space discovered, leaves autoenrollment untouched

    Given a CF instance with the following orgs, spaces
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |

  Scenario: Preexisting service customer autosleep instances left untouched by autoenrollment

    Given a CF instance with the following orgs, spaces
      | org        | private domains | spaces |
      | team-a-dev | portal.acme.com | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params) |
      | team-a-dev | portal | curious-autosleep-tester()                     |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                                    |
      | team-a-dev | portal | curious-autosleep-tester() , autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |


  Scenario: transient opt out from space enrollment (enrollspace.mode=forced)

    Given a CF instance with the following orgs, spaces
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
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
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |


  Scenario: backoffice management of org and space enrollment


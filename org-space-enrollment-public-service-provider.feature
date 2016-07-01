Feature: public paas service provider org and space autoenrollment

  A public service provider offers CF self service mode, where customers create new orgs and spaces (choosing how to
  name them), and may delete spaces, and orgs (i.e. without autosleep preventing such self-service org/space mgt)

  The service provider would like to have autosleep automatically applied with default configuration on all new orgs and spaces created.

  In addition, upon commercial negociations with customers, the service provider needs to tune the autosleep
  configuration on a customer per customer basis (e.g. exclude orgs, spaces, apps, or tune idle duration).
  This is done through a backoffice REST API

  The autosleep service (default service plan) is made visible to all orgs in the market place. This is useful for customers
  to access documentation associated with autosleep service instances that appear in their space and understand the actions
  performed on their applications.

  Service consummers are not given capability to disable autosleep or configure autosleep
  (e.g. exclude apps, tune idle duration) in a self service mode. The service provider therefore configures the
  the forced space enrollment mode in which opt-outs are only transient, leaving a fair timeslot to delete the autosleep
  service instance (which is necessary to delete a space).

  While service consummer may create new autosleep service instances in their space, the service-provider-instanciated
  service instance still applies. Service-consummer-instanciated autosleep service instance would in effect only reduce
  idle duration of enrolled apps.

  Applications autowakeup is applied on all shared domains provided by the service providers (with the preq that shared
  domains wildcard route get mapped to the autosleep application). Application autowakeup on private domains would need
  service consummers actions to route orphan traffic to the autosleep public route.

  service-provider back office REST API endpoints (inspired from https://github.com/cloudfoundry/cc-api-v3-style-guide/blob/master/README.md )

  enrolled-orgs/guid PUT (empty body) enroll a given org with default autosleep configuration
  enrolled-orgs/guid PUT (json body) enroll a given org with specific autosleep configuration
  enrolled-orgs/guid DELETE: unenroll a given org
  enrolled-orgs/guid GET: access default space config for an org
  { idle-duration=T24H, exclude-from-auto-enrollment="" auto-enrollment=standard }
  enrolled-orgs/guid PATCH (json body) modify default space config for an org

  enrolled-space/guid PUT (empty body): enroll with org default space config.
  enrolled-space/guid DELETE: unenroll a given space
  enrolled-space/guid GET: get space autosleep config (persistent to transient opt-out)
  { idle-duration=T10H, exclude-from-auto-enrollment="", auto-enrollment=standard, secret=Th1s1zg00dP@$$w0rd }
  enrolled-space/guid PATCH(json body): change space autosleep config (persistent to transient opt-out)


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
      enrollspace.exclude-orgs=system_domain|premium
      enrollspace.include-space=*
      enrollspace.exclude-space=
      #Exclude shared from autowakeup registration
      enrollspace.exclude-domain=portal.acme.com


      #Specify autosleep service-instance default configuration (as arbitrary params) applied on every auto enrolled space,
      # or to opt-ins when no arbitrary params are set

      #Specify autosleep config in enrolled spaces
      #Override default 24H idle duration
      default.idle-duration=T10H
      #Choose apps to exclude from enrollment
      default.exclude-from-auto-enrollment=
      #app enrollment mode among: standard, forced.
      default.auto-enrollment=standard
      #default.secret=Th1s1zg00dP@$$w0rd

      """


  Scenario: new org and space discovered triggers default config autoenrollment

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org           | spaces             |
      | system_domain | autosleep,admin-ui |
      | team-a-prod   | portal             |
      | team-a-dev    | portal             |
    And the autosleep service instances in each space are
      | org           | space     | autosleep service instances (arbitrary params) |
      | system_domain | autosleep |                                                |
      | system_domain | admin-ui  |                                                |
      | team-a-prod   | portal    |                                                |
      | team-a-dev    | portal    |                                                |

    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org           | space     | autosleep service instances (arbitrary params)                       |
      | system_domain | autosleep |                                                                      |
      | system_domain | admin-ui  |                                                                      |
      | team-a-prod   | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-a-dev    | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |

  Scenario: backoffice management of org and space enrollment

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org(guid)        | spaces(guid)  |
      | system_domain(0) | autosleep(90) |
      | team-a-prod(1)   | portal(100)   |
      | team-a-dev(2)    | portal(101)   |
      | premium(3)       | portal(102)   |
    And the autosleep service instances in each space are
      | org           | space     | autosleep service instances (arbitrary params) |
      | system_domain | autosleep |                                                |
      | team-a-prod   | portal    |                                                |
      | team-a-dev    | portal    |                                                |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                       |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
      | team-b-dev  | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    And the backoffice REST API returns
      | endpoint           | method | body                                                                           |
      | enrolled-orgs/1    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=standard } |
      | enrolled-orgs/2    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=standard } |
      | enrolled-space/100 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=standard } |
      | enrolled-space/101 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=standard } |

    When a backoffice change is made through
      | endpoint           | method | body |
      | enrolled-space/101 | DELETE |      |
    Then the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |

    When a backoffice change is made through
      | endpoint           | method | body                                                                           |
      | enrolled-space/101 | PATCH  | {idle-duration=T48H, exclude-from-auto-enrollment=, auto-enrollment=standard } |
    Then the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                       |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=standard) |


    When a backoffice change is made through
      | endpoint       | method | body |
      | enrolled-org/3 | PUT    |      |
    Then the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                       |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=standard) |
      | premium     | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                       |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=standard) |
      | premium     | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |


  Scenario: no new org and space discovered, leaves autoenrollment untouched

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
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

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
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

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
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



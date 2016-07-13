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
  domains wildcard route get mapped to the autowakeup application by service provider).
  Application autowakeup on private domains would either need service consummers actions to route orphan traffic to the
  autowakeup public route, or could be automated by the autoenrolment feature.
  Autosleep autoenrollment could automate this processing:
  - create a dedicated "service-consummer-autowakeup" space in the service consummer org,
  - create wildcard route for customers private domains in the "service-consummer-autowakeup" space
  - automatically push the zuul proxy in the "service-consummer-autowakeup" space.
  Service consummers could potentially alter the "service-consummer-autowakeup" to filter out confidential traffic from
  autowakeup, or disable the traffic proxying to autowakeup, resulting in sleeping apps not being waken up anymore.

  service-provider back office REST API endpoints (inspired from
  https://github.com/cloudfoundry/cc-api-v3-style-guide/blob/master/README.md ) which are used to construct
  backoffice UIs

  orgs/ GET
  { org guid=   state=enrolled|opted_out href=/enrolled-orgs/guid }
  enrolled-orgs/guid PUT (empty body) enroll a given org with default autosleep configuration
  enrolled-orgs/guid PUT (json body) enroll a given org with specific autosleep configuration
  enrolled-orgs/guid DELETE: unenroll a given org
  enrolled-orgs/guid GET: access default space config for an org
  { idle-duration=T24H, exclude-from-auto-enrollment="" auto-enrollment=standard }
  enrolled-orgs/guid PATCH (json body) modify default space config for an org

  spaces/ GET

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

      #one of: disabled (no enrollment), standard (permanent opt-out possible), forced (only transient opt-out)
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
      #app enrollment mode among:
      # - standard (allows permanent app opt-outs, autosleep service instance deletion accepted)
      # - forced (autosleep service instance deletion rejected).
      # - transient-opt-outs (allows transient app opt-outs, autosleep service instance deletion accepted)

      # the service provider is selecting in this case transient-opt-outs, so that service consummers can delete the autosleep
      # service instance, but only transiently opt-out (i.e. unbind from their app.)
      default.auto-enrollment=transient-opt-outs
      #default.secret=Th1s1zg00dP@$$w0rd

      cf.service.description=Automatically puts inactive apps to sleep (SAP controlled)
      """

  Scenario: marketplace exposes autosleep to paas consummers
    Then marketplace contains
      | service   | plans   | description                                                |
      | autosleep | default | Automatically puts inactive apps to sleep (SAP controlled) |
    And the command "cf service autosleep" displays
      """
      cf service autosleep

      Service instance: autosleep
      Service: autosleep
      Bound apps: autosleep-broker,autowake-proxy
      Tags:
      Plan: default
      Description: Automatically puts inactive apps to sleep (SAP controlled)
      Documentation url: http://docs.sap-cloud.com/autosleep-manual
      Dashboard: https://docs.sap-cloud.com/en-US/app/search/flashtimeline?q=search%20index%3D%22*%22%20source%3D%22tcp:12345%22

      Last Operation
      Status: create succeeded
      Message:
      Started: 2016-06-14T15:17:31Z
      Updated:
    """
    And the manual urls states:
    """
      Autosleep service instance is internally used by SAP to optimize resources. While new service instances may be created
      it is not possible to increase the idle duration without contacting SAP sales
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
      | org           | space     | autosleep service instances (arbitrary params)                                 |
      | system_domain | autosleep |                                                                                |
      | system_domain | admin-ui  |                                                                                |
      | team-a-prod   | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
      | team-a-dev    | portal    | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |

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
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
      | team-b-dev  | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    And the backoffice REST API returns
      | endpoint           | method | body                                                                                                                    |
      | orgs               | GET    | {[org guid=1 state=enrolled, href=/enrolled-orgs/1], [org guid=2 state=enrolled, href=/enrolled-orgs/2]}                |
      | spaces             | GET    | { [space guid=100 state=enrolled, href=/enrolled-space/100], [space guid=101 state=enrolled, href=/enrolled-space/101]} |
      | enrolled-orgs/1    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                |
      | enrolled-orgs/2    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                |
      | enrolled-space/100 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                |
      | enrolled-space/101 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                |

    When a backoffice change is made through
      | endpoint           | method | body |
      | enrolled-space/101 | DELETE |      |
    Then the backoffice REST API returns
      | endpoint           | method | body                                                                                                     |
      | orgs               | GET    | {[org guid=1 state=enrolled, href=/enrolled-orgs/1], [org guid=2 state=enrolled, href=/enrolled-orgs/2]} |
      | spaces             | GET    | { [space guid=100 state=enrolled, href=/enrolled-space/100], [space guid=101 state=opted-out]}           |
      | enrolled-orgs/1    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                 |
      | enrolled-orgs/2    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                 |
      | enrolled-space/100 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                 |
      | enrolled-space/101 | GET    | empty (404 status)                                                                                       |
    Then the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |

    When a backoffice change is made through
      | endpoint           | method | body                                                                                     |
      | enrolled-space/100 | PATCH  | {idle-duration=T48H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs } |
    Then the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=transient-opt-outs) |


    When a backoffice change is made through
      | endpoint       | method | body |
      | enrolled-org/3 | PUT    |      |
    Then the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=transient-opt-outs) |
      | premium     | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org         | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-prod | portal | autosleep-autoenrolled(idle-duration=T48H, auto-enrollment=transient-opt-outs) |
      | premium     | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    And the backoffice REST API returns
      | endpoint           | method | body                                                                                                                                                          |
      | orgs               | GET    | {[org guid=1 state=enrolled, href=/enrolled-orgs/1], [org guid=2 state=enrolled, href=/enrolled-orgs/2] , [org guid=3 state=enrolled, href=/enrolled-orgs/3]} |
      | spaces             | GET    | { [space guid=100 state=enrolled, href=/enrolled-space/100], [space guid=101 state=opted-out], [space guid=102 state=enrolled, href=/enrolled-space/102]}     |
      | enrolled-orgs/1    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                                                      |
      | enrolled-orgs/2    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                                                      |
      | enrolled-orgs/3    | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                                                      |
      | enrolled-space/100 | GET    | {idle-duration=T48H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                                                      |
      | enrolled-space/102 | GET    | {idle-duration=T10H, exclude-from-auto-enrollment=, auto-enrollment=transient-opt-outs }                                                                      |


  Scenario: no new org and space discovered, leaves autoenrollment untouched

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |

  Scenario: Preexisting service customer autosleep instances left untouched by autoenrollment

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org        | private domains | spaces |
      | team-a-dev | portal.acme.com | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                                                                                                              | bound apps        |
      | team-a-dev | portal | curious-autosleep-tester(idle-duration=T365D) curious-autosleep-tester2(idle-duration=T365D, auto-enrollment=transient-opt-outs, exclude-from-autoenrollment=curious-app* ) | app1, curious-app |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                                                                                                  |
      | team-a-dev | portal | curious-autosleep-tester(idle-duration=T365D auto-enrollment=transient-optouts), autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    And the enrolled apps are:
      | org        | space  | app         | bound service instances                                                     |
      | team-a-dev | portal | app1        | curious-autosleep-tester, curious-autosleep-tester2, autosleep-autoenrolled |
      | team-a-dev | portal | curious-app | autosleep-autoenrolled                                                      |


  Scenario: transient opt out from space enrollment (enrollspace.mode=forced)

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |
    When the autosleep-autoenrolled service instance is deleted by service consummer
    Then the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params) |
      | team-a-dev | portal |                                                |
    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |


  Scenario: app opts out are transient
    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org        | spaces |
      | team-a-dev | portal |
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 | bound apps |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) | app1       |
    When an app app1 is unbound
    Then the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 | bound apps |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) |            |
    When clock reaches the app scan date
    Then the app gets enrolled again
    And the autosleep service instances in each space are
      | org        | space  | autosleep service instances (arbitrary params)                                 | bound apps |
      | team-a-dev | portal | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=transient-opt-outs) | app1       |


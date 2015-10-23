package org.cloudfoundry.autosleep.client;

import org.cloudfoundry.autosleep.client.model.AbstractEntity;
import org.cloudfoundry.autosleep.client.model.AppEntity;
import org.cloudfoundry.autosleep.client.model.CloudfoundryObject;
import org.cloudfoundry.autosleep.client.model.OAuthCredentials;
import org.cloudfoundry.autosleep.client.model.OrganizationEntity;
import org.cloudfoundry.autosleep.client.model.SpaceEntity;

public interface CloudFoundryApiClientService {

    void initCredential(String username, String password) throws CloudFoundryException;

    void setCredentials(String refreshToken) throws CloudFoundryException;

    OAuthCredentials getCredentials();

    void logout();

    /**
     * Interface used to read remote objects.
     *
     * @param <T> the class that will be read
     */
    interface CloudfoundryObjectReader<T extends AbstractEntity> {

        /**
         *Function called on each remote object.
         *
         * @param object the remote object
         * @return true if iteration must go on, false otherwise
         * @throws CloudFoundryException Will stop iteration and put the function in error
         */
        boolean read(CloudfoundryObject<T> object) throws CloudFoundryException;
    }

    void readOrganizations(CloudfoundryObjectReader<OrganizationEntity> reader) throws CloudFoundryException;

    void readSpaces(CloudfoundryObject<OrganizationEntity> organization, CloudfoundryObjectReader<SpaceEntity> reader)
            throws CloudFoundryException;

    void readApps(CloudfoundryObject<SpaceEntity> space, CloudfoundryObjectReader<AppEntity> reader)
            throws CloudFoundryException;

    CloudfoundryObject<AppEntity> startApp(CloudfoundryObject<AppEntity> app) throws CloudFoundryException;

    CloudfoundryObject<AppEntity> stopApp(CloudfoundryObject<AppEntity> app) throws CloudFoundryException;

}

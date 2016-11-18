/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.license.server;

import com.codenvy.api.license.server.dao.CodenvyLicenseDao;
import com.codenvy.api.license.server.jpa.JpaCodenvyLicenseDao;
import com.google.inject.AbstractModule;

import org.eclipse.che.inject.DynaModule;

/**
 * @author Alexander Andrienko
 */
@DynaModule
public class LicenseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LicenseService.class);
        bind(LicenseServicePermissionsFilter.class);
        bind(CodenvyLicenseDao.class).to(JpaCodenvyLicenseDao.class);
    }
}

/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package org.ofbiz.jcr.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.JcrRepositoryAccessor;
import org.ofbiz.jcr.access.jackrabbit.JackrabbitRepositoryAccessor;
import org.ofbiz.jcr.api.JcrDataHelper;
import org.ofbiz.jcr.api.JcrFileHelper;
import org.ofbiz.jcr.api.jackrabbit.JackrabbitArticleHelper;
import org.ofbiz.jcr.api.jackrabbit.JackrabbitFileHelper;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitArticle;
import org.ofbiz.jcr.util.jackrabbit.JcrUtilJackrabbit;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.testtools.OFBizTestCase;

public class JackrabbitTests extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public JackrabbitTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));

    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testRepositoryConstructor() throws Exception {
        JcrRepositoryAccessor repositoryAccess = new JackrabbitRepositoryAccessor(userLogin);
        assertNotNull(repositoryAccess);
    }

    public void testCrudArticleNode() throws Exception {
        // Create New Object
        JcrDataHelper helper = new JackrabbitArticleHelper(userLogin);
        helper.storeContentInRepository("news/article", "en", "News Of Today", "Hello World", new GregorianCalendar());

        JackrabbitArticle content = helper.readContentFromRepository("news/article");
        assertEquals("Hello World", content.getContent());

        content.setContent("New World!");

        helper.updateContentInRepository(content);

        JackrabbitArticle updatedContent = helper.readContentFromRepository("news/article");
        assertEquals("New World!", updatedContent.getContent());

        helper.removeContentObject("news");

        helper.closeContentSession();
    }

    public void testVersionning() throws Exception {
        JcrDataHelper helper = new JackrabbitArticleHelper(userLogin);
        helper.storeContentInRepository("news/versionArticle", "en", "News Of Today", "Hello World", new GregorianCalendar());

        JackrabbitArticle content = helper.readContentFromRepository("news/versionArticle");
        assertEquals("1.0", content.getVersion());

        content.setTitle("New Title");
        helper.updateContentInRepository(content);

        content = helper.readContentFromRepository("news/versionArticle");
        assertEquals("1.1", content.getVersion());

        helper.removeContentObject("news");

        helper.closeContentSession();
    }

    public void testLanguageDetermination() throws Exception {
        JcrDataHelper helper = new JackrabbitArticleHelper(userLogin);

        helper.storeContentInRepository("news/tomorrow", "en", "The news for tomorrow.", "Content.", new GregorianCalendar());
        helper.storeContentInRepository("superhero", "de", "Batman", "The best superhero!", new GregorianCalendar());

        assertEquals("en", helper.readContentFromRepository("/news/tomorrow", "").getLanguage());
        assertEquals("en", helper.readContentFromRepository("/news/tomorrow", "de").getLanguage());
        assertEquals("en", helper.readContentFromRepository("/news/tomorrow", "en").getLanguage());

        assertEquals("de", helper.readContentFromRepository("/superhero", "de").getLanguage());
        assertEquals("de", helper.readContentFromRepository("/superhero", "").getLanguage());
        assertEquals("de", helper.readContentFromRepository("/superhero", "fr").getLanguage());

        helper.removeContentObject("/superhero");
        helper.removeContentObject("/news");
        helper.closeContentSession();
    }

    /*
     * Test the File upload
     */
    public void testCreateRepositoryFileNode() throws Exception {
        File f = new File("stopofbiz.sh");
        File f2 = new File("README");
        assertTrue(f.exists() && f2.exists());

        InputStream file = new FileInputStream(f);

        JcrFileHelper helper = new JackrabbitFileHelper(userLogin);
        helper.storeContentInRepository(file, f.getName(), "/fileHome");

        assertNotNull(helper.getRepositoryContent("/fileHome/" + f.getName()));

        // add a second file to the same folder
        file = new FileInputStream(f2);

        helper.storeContentInRepository(file, f2.getName(), "/fileHome");
        assertNotNull(helper.getRepositoryContent("/fileHome/" + f2.getName()));

        // remove all files in folder
        helper.removeContentObject("/fileHome");

        helper.closeContentSession();
    }

    public void testQuery() throws Exception {
        JcrDataHelper helper = new JackrabbitArticleHelper(userLogin);

        helper.storeContentInRepository("/query", "en", "query", "query test", new GregorianCalendar());

        List<Map<String, String>> queryResult = helper.queryData("SELECT * FROM [nt:unstructured]");

        assertEquals(3, queryResult.size()); // the list should contain 3 result sets

        assertEquals("/", queryResult.get(0).get("path"));
        assertEquals("/query", queryResult.get(1).get("path"));
        assertEquals("/query/en", queryResult.get(2).get("path"));

        helper.removeContentObject("query");

        helper.closeContentSession();

    }

    public void testSpeedTestService() throws Exception {
        Map<String, Object> context = FastMap.newInstance();
        context.put("maxNodes", new Integer(10));
        context.put("userLogin", dispatcher.getDelegator().findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system")));

        Map<String, Object> serviceResult = this.dispatcher.runSync("determineJackrabbitRepositorySpeed", context);

        if (ServiceUtil.isError(serviceResult)) {
            assertFalse(true);
        } else {
            assertTrue(true);
        }

    }

    public void testListRepositoryNodes() throws Exception {
        assertNotNull(JcrUtilJackrabbit.getRepositoryNodes(userLogin, null));
    }

}
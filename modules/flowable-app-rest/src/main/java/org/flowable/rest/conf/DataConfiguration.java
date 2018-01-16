/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.conf;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
@Configuration
public class DataConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DataConfiguration.class);

    @Autowired
    protected IdentityService identityService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected TaskService taskService;

    @PostConstruct
    public void init() {

        LOGGER.info("Initializing groups");
        initGroups();
        LOGGER.info("Initializing users");
        initUsers();

        LOGGER.info("Initializing WorkFlows definitions");
        initWFDefinitions();

//        LOGGER.info("Initializing models");
//        initDemoModelData();
    }

    protected void initGroups() {
        String[] assignmentGroups = new String[] { "student", "backoffice"};
        for (String groupId : assignmentGroups) {
            createGroup(groupId, "assignment");
        }

        String[] securityGroups = new String[] { "user", "admin" };
        for (String groupId : securityGroups) {
            createGroup(groupId, "security-role");
        }
    }

    protected void createGroup(String groupId, String type) {
        if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
            Group newGroup = identityService.newGroup(groupId);
            newGroup.setName(groupId.substring(0, 1).toUpperCase() + groupId.substring(1));
            newGroup.setType(type);
            identityService.saveGroup(newGroup);
        }
    }

    protected void initUsers() {
//        createUser("kermit", "Kermit", "The Frog", "kermit", "kermit@flowable.org", null, Arrays.asList("student", "backoffice", "user", "admin"),
//                Arrays.asList("birthDate", "10-10-1955", "jobTitle", "Muppet", "location", "Hollywood", "phone", "+123456789", "twitterName", "alfresco", "skype", "flowable_kermit_frog"));
//
//        createUser("gonzo", "Gonzo", "The Great", "gonzo", "gonzo@flowable.org", null, Arrays.asList("student", "backoffice", "user", "admin"), null);
        createUser("admin", "admin", "admin", "admin", "francisco.mantaras@santexgroup.com", null, Arrays.asList("student", "backoffice", "user", "admin"), null);
    }

    protected void createUser(String userId, String firstName, String lastName, String password, String email, String imageResource, List<String> groups, List<String> userInfo) {

        if (identityService.createUserQuery().userId(userId).count() == 0) {

            // Following data can already be set by demo setup script

            User user = identityService.newUser(userId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            user.setEmail(email);
            identityService.saveUser(user);

            if (groups != null) {
                for (String group : groups) {
                    identityService.createMembership(userId, group);
                }
            }
        }

        // Following data is not set by demo setup script

        // image
        if (imageResource != null) {
            byte[] pictureBytes = IoUtil.readInputStream(this.getClass().getClassLoader().getResourceAsStream(imageResource), null);
            Picture picture = new Picture(pictureBytes, "image/jpeg");
            identityService.setUserPicture(userId, picture);
        }

        // user info
        if (userInfo != null) {
            for (int i = 0; i < userInfo.size(); i += 2) {
                identityService.setUserInfo(userId, userInfo.get(i), userInfo.get(i + 1));
            }
        }

    }

    protected void initWFDefinitions() {

        String deploymentName = "WorkFlow processes";
        List<Deployment> deploymentList = repositoryService.createDeploymentQuery().deploymentName(deploymentName).list();

        if (deploymentList == null || deploymentList.isEmpty()) {
            repositoryService.createDeployment().name(deploymentName)
            .addClasspathResource("bannerCreationProcess.bpmn").addClasspathResource("crmCreationProcess.bpmn").addClasspathResource("crmGetProcess.bpmn")
            .addClasspathResource("crmUpdateProcess.bpmn").addClasspathResource("notificationProcess.bpmn").addClasspathResource("userNotificationProcess.bpmn")
            .addClasspathResource("csuChange.bpmn").addClasspathResource("devolutionPostponement.bpmn").addClasspathResource("modalityChange.bpmn")
            .addClasspathResource("programChange.bpmn").addClasspathResource("reAdmission.bpmn").addClasspathResource("requestForms.bpmn")
            .addClasspathResource("scheduleChange.bpmn").addClasspathResource("siteChange.bpmn").addClasspathResource("studentIdentification.bpmn")
            .addClasspathResource("supplementaryExam.bpmn").deploy();
        }
    }

    protected void initDemoModelData() {
        createModelData("Demo model", "This is a demo model", "org/flowable/rest/demo/model/test.model.json");
    }

    protected void createModelData(String name, String description, String jsonFile) {
        List<Model> modelList = repositoryService.createModelQuery().modelName("Demo model").list();

        if (modelList == null || modelList.isEmpty()) {

            Model model = repositoryService.newModel();
            model.setName(name);

            ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
            modelObjectNode.put("name", name);
            modelObjectNode.put("description", description);
            model.setMetaInfo(modelObjectNode.toString());

            repositoryService.saveModel(model);

            try {
                InputStream svgStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/rest/demo/model/test.svg");
                repositoryService.addModelEditorSourceExtra(model.getId(), IOUtils.toByteArray(svgStream));
            } catch (Exception e) {
                LOGGER.warn("Failed to read SVG", e);
            }

            try {
                InputStream editorJsonStream = this.getClass().getClassLoader().getResourceAsStream(jsonFile);
                repositoryService.addModelEditorSource(model.getId(), IOUtils.toByteArray(editorJsonStream));
            } catch (Exception e) {
                LOGGER.warn("Failed to read editor JSON", e);
            }
        }
    }

    protected String randomSentence(String[] words, int length) {
        Random random = new Random();
        StringBuilder strb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            strb.append(words[random.nextInt(words.length)]);
            strb.append(" ");
        }
        return strb.toString().trim();
    }

}

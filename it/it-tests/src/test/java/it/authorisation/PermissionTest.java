/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsPermissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddGroupToTemplateWsRequest;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserToTemplateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.CreateTemplateWsRequest;
import org.sonarqube.ws.client.permission.GroupsWsRequest;
import org.sonarqube.ws.client.permission.PermissionsWsClient;
import org.sonarqube.ws.client.permission.RemoveGroupFromTemplateWsRequest;
import org.sonarqube.ws.client.permission.RemoveUserFromTemplateWsRequest;
import org.sonarqube.ws.client.permission.SearchTemplatesWsRequest;
import org.sonarqube.ws.client.permission.UsersWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.WsRequest.newPostRequest;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class PermissionTest {
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;
  private static PermissionsWsClient permissionsWsClient;

  private static final String PROJECT_KEY = "sample";
  private static final String LOGIN = "george.orwell";
  private static final String GROUP_NAME = "1984";

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/authorisation/one-issue-per-line-profile.xml"));

    orchestrator.getServer().provisionProject(PROJECT_KEY, "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarRunner sampleProject = SonarRunner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sampleProject);

    adminWsClient = newAdminWsClient(orchestrator);
    permissionsWsClient = adminWsClient.permissionsClient();

    createUser(LOGIN, "George Orwell");
    createGroup(GROUP_NAME);
  }

  @AfterClass
  public static void delete_data() {
    deactivateUser(LOGIN);
    deleteGroup(GROUP_NAME);
  }

  @Test
  public void permission_web_services() {
    permissionsWsClient.addUser(
      new AddUserWsRequest()
        .setPermission("admin")
        .setLogin(LOGIN));
    permissionsWsClient.addGroup(
      new AddGroupWsRequest()
        .setPermission("admin")
        .setGroupName(GROUP_NAME));

    WsPermissions.WsSearchGlobalPermissionsResponse searchGlobalPermissionsWsResponse = permissionsWsClient.searchGlobalPermissions();
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getKey()).isEqualTo("admin");
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getUsersCount()).isEqualTo(1);
    // by default, a group has the global admin permission
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getGroupsCount()).isEqualTo(2);

    WsPermissions.UsersWsResponse users = permissionsWsClient
      .users(new UsersWsRequest()
        .setPermission("admin"));
    assertThat(users.getUsersList()).extracting("login").contains(LOGIN);

    WsPermissions.WsGroupsResponse groupsResponse = permissionsWsClient
      .groups(new GroupsWsRequest()
        .setPermission("admin"));
    assertThat(groupsResponse.getGroupsList()).extracting("name").contains(GROUP_NAME);
  }

  @Test
  public void template_permission_web_services() {
    WsPermissions.CreateTemplateWsResponse createTemplateWsResponse = permissionsWsClient.createTemplate(
      new CreateTemplateWsRequest()
        .setName("my-new-template")
        .setDescription("template-used-in-tests"));
    assertThat(createTemplateWsResponse.getPermissionTemplate().getName()).isEqualTo("my-new-template");

    permissionsWsClient.addUserToTemplate(
      new AddUserToTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    permissionsWsClient.addGroupToTemplate(
      new AddGroupToTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    SearchTemplatesWsResponse searchTemplatesWsResponse = permissionsWsClient.searchTemplates(
      new SearchTemplatesWsRequest()
        .setQuery("my-new-template"));
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getName()).isEqualTo("my-new-template");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getKey()).isEqualTo("admin");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getUsersCount()).isEqualTo(1);
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getGroupsCount()).isEqualTo(1);

    permissionsWsClient.removeGroupFromTemplate(
      new RemoveGroupFromTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    permissionsWsClient.removeUserFromTemplate(
      new RemoveUserFromTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    SearchTemplatesWsResponse clearedSearchTemplatesWsResponse = permissionsWsClient.searchTemplates(
      new SearchTemplatesWsRequest()
        .setQuery("my-new-template"));
    assertThat(clearedSearchTemplatesWsResponse.getPermissionTemplates(0).getPermissionsList()).isEmpty();
  }

  private static void createUser(String login, String name) {
    adminWsClient.execute(
      newPostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("password", "123456"));
  }

  private static void deactivateUser(String login) {
    adminWsClient.execute(
      newPostRequest("api/users/deactivate")
        .setParam("login", login));
  }

  private static void createGroup(String groupName) {
    adminWsClient.execute(
      newPostRequest("api/user_groups/create")
        .setParam("name", groupName));
  }

  private static void deleteGroup(String groupName) {
    adminWsClient.execute(
      newPostRequest("api/user_groups/delete")
        .setParam("name", groupName));
  }
}
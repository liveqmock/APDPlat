/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.apdplat.module.security.action;

import net.sf.json.JSONArray;
import org.apdplat.module.security.model.Position;
import org.apdplat.module.security.model.Role;
import org.apdplat.module.security.model.User;
import org.apdplat.module.security.model.UserGroup;
import org.apdplat.module.system.service.PropertyHolder;
import org.apdplat.platform.action.ExtJSSimpleAction;
import org.apdplat.platform.criteria.Property;
import org.apdplat.platform.criteria.PropertyCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.apdplat.module.security.service.UserReportService;
import org.apdplat.module.security.service.UserService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

@Scope("prototype")
@Controller
@RequestMapping("/security")
public class UserAction extends ExtJSSimpleAction<User> {
    private int orgId;
    private String oldPassword;
    private String newPassword;
    private String roles;
    private String positions;
    private String userGroups;
    
    //在线用户 根据org查找
    private String org;
    //在线用户 根据role查找
    private String role;
    //用户重置密码
    private String password;
    
    //用户选择组件
    private boolean select;
    
    @Resource(name="userReportService")
    private UserReportService userReportService;
    @Resource(name="userService")
    private UserService userService;
    
    @Override
    @ResponseBody
    public String report(){
        byte[] report = userReportService.getReport(servletContext, getRequest());
        return new String(report);
    }
    @Override
    protected void checkModel(User model) throws Exception{
        userService.checkModel(model);
    }
    
    @Override
    public PropertyCriteria buildPropertyCriteria(){
        return userService.buildPropertyCriteria(super.buildPropertyCriteria(), orgId);
    }

    @ResponseBody
    public String reset(){
        String result = userService.reset(getIds(), password);
        return result;
    }

    @ResponseBody
    public String online(){
        page = userService.getOnlineUsers(getStart(), getLimit(), org, role);
        
        Map data = new HashMap();
        data.put("totalProperty", page.getTotalRecords());
        List<Map> result = new ArrayList<>();
        renderJsonForQuery(result);
        data.put("root", result);
        String json = JSONArray.fromObject(data).toString();
        return json;
    }

    @ResponseBody
    public String store(){
        if(select){
            return super.query();
        }
        List<User> users=getService().query(User.class).getModels();
        List<Map<String,String>> data=new ArrayList<>();
        users.forEach(user -> {
            Map<String,String> temp=new HashMap<>();
            temp.put("value", user.getUsername());
            temp.put("text", user.getUsername());
            data.add(temp);
        });
        String json = JSONArray.fromObject(data).toString();
        return json;
    }
    @Override
    public void assemblyModelForCreate(User model) {
        userService.assemblyModelForCreate(model, roles, positions, userGroups);
    }
    @Override
    public void prepareForDelete(Integer[] ids){
        userService.prepareForDelete(ids);
    }   
    @Override
    protected void old(User model) {
        if(PropertyHolder.getBooleanProperty("demo")){
            if(model.getUsername().equals("admin")){
                throw new RuntimeException("演示版本不能修改admin用户");
            }
        }
    }
    @ResponseBody
    public String modifyPassword(){
        Map data = userService.modifyPassword(oldPassword, newPassword);
        String json = JSONArray.fromObject(data).toString();
        return json;
    }
    
    // 在更新一个特定的部分的Model之前对Model添加需要修改的属性
    @Override
    protected void assemblyModelForPartUpdate(List<Property> properties) {
        userService.assemblyModelForPartUpdate(properties, model);
    }
    @Override
    protected void assemblyModelForUpdate(User model){
        userService.assemblyModelForUpdate(model, roles, positions, userGroups);
    }
    @Override
    protected void renderJsonForRetrieve(Map map) {
        render(map,model);
        map.put("roles", model.getRoleStrs());
        map.put("positions", model.getPositionStrs());
        map.put("userGroups", model.getUserGroupStrs());
    }
    
    @Override
    protected void renderJsonForSearch(List result) {
        for (User user : page.getModels()) {
            Map temp = new HashMap();
            render(temp,user);

            StringBuilder str=new StringBuilder();
            //搜索出来的模型已经被detach了，无法获得延迟加载的数据
            User tmp=getService().retrieve(User.class, user.getId());
            tmp.getRoles().forEach(r -> {
                str.append(r.getRoleName()).append(",");
            });
            temp.put("roles", str.length() > 1 ? str.toString().substring(0, str.length() - 1) : "");

            str.setLength(0);
            tmp.getPositions().forEach(p -> {
                str.append(p.getPositionName()).append(",");
            });
            temp.put("positions", str.length() > 1 ? str.toString().substring(0, str.length() - 1) : "");
            result.add(temp);
            
            str.setLength(0);
            tmp.getUserGroups().forEach(p -> {
                str.append(p.getUserGroupName()).append(",");
            });
            temp.put("userGroups", str.length() > 1 ? str.toString().substring(0, str.length()-1):"");
            result.add(temp);
        }
    }
    @Override
    protected void renderJsonForQuery(List result) {
        for (User user : page.getModels()) {
            //重新加载，避免出现延迟加载错误
            user = getService().retrieve(modelClass, user.getId());
            Map temp = new HashMap();
            render(temp,user);

            StringBuilder str=new StringBuilder();
            user.getRoles().forEach(r -> {
                str.append(r.getRoleName()).append(",");
            });
            temp.put("roles", str.length() > 1 ? str.toString().substring(0, str.length() - 1) : "");

            str.setLength(0);
            user.getPositions().forEach(p -> {
                str.append(p.getPositionName()).append(",");
            });
            temp.put("positions", str.length() > 1 ? str.toString().substring(0, str.length() - 1) : "");
            result.add(temp);

            str.setLength(0);
            user.getUserGroups().forEach(p -> {
                str.append(p.getUserGroupName()).append(",");
            });
            temp.put("userGroups", str.length()>1?str.toString().substring(0, str.length()-1):"");
            result.add(temp);
        }
    }
    @Override
    protected void render(Map map,User model){
        map.put("id", model.getId());
        map.put("version", model.getVersion());
        map.put("username", model.getUsername());
        map.put("realName", model.getRealName());
        map.put("enabled", model.isEnabled()==true?"启用":"停用");
        String orgName="";
        int id=0;
        if(model.getOrg()!=null){
            orgName=model.getOrg().getOrgName();
            id=model.getOrg().getId();
        }
        map.put("orgName", orgName);
        map.put("orgId", id+"");
        map.put("des", model.getDes());
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void setPositions(String positions) {
        this.positions = positions;
    }

    public void setUserGroups(String userGroups) {
        this.userGroups = userGroups;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }
}
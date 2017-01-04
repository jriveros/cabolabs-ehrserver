package ehr

import com.cabolabs.ehrserver.openehr.directory.Folder
import com.cabolabs.ehrserver.openehr.ehr.Ehr
import com.cabolabs.security.Role

class EhrTagLib {
   
   def springSecurityService

   def hasEhr = { attrs, body ->
      
      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")
      
      //println patientUID
      
      def c = Ehr.createCriteria()
      
      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }
      
      //println ehr
      
      if (ehr) out << body()
   }
   
   def dontHasEhr = { attrs, body ->

      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")
      
      //println attrs.patientUID
      
      def c = Ehr.createCriteria()
      
      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }
      
      //println ehr
      
      if (!ehr) out << body()
   }
   
   def ehr_directory = { attrs, body ->
      
      if (!attrs.directory) return
      
      out << recursive_directory(attrs.directory)
   }
   
   private String recursive_directory(Folder folder)
   {
      def html = $/
      |<div class="folder">
      |  <div class="folder_name">
      |    <input type="radio" name="folder.id" value="${folder.id}" />
      |    ${folder.name} (${folder.items.size()})
      |  </div>
      |  <div class="folder_items">
      /$.stripMargin()

      folder.items.each {
         // Versioned Composition: UID
         html += '<div class="folder_item">Versioned Composition: '+ g.link(controller:'versionedComposition', action:'show', params:[uid:it], it) +'</div>'
      }
      
      html += '</div><div class="folder_folders">'
      
      folder.folders.each {
         html += recursive_directory(it)
      }
      
      html += '</div></div>' // /folder_folders, /folder
      
      return html
   }
   
   
   /**
    * Renders a select with the organizations of the current logged user.
    * This is used in the query test UID to filter by organizationUid.
    */
   def selectWithCurrentUserOrganizations = { attrs, body ->
      
      def loggedInUser = springSecurityService.currentUser
      if(loggedInUser)
      {
         def args = [:]
         args.from = loggedInUser.organizations
         args.optionKey = 'uid'
         args.optionValue = {it.name +' '+ it.uid} //'name'
         args.noSelection = ['':'Select One...'] // TODO: i18n
         args.class = attrs.class ?: '' // allows set style from outside
         
         if (attrs.multiple)
         {
            args.multiple = 'true'
            args.size = 5
         }
         
         // add the rest of the attrs to the select args, name, value, class, etc
         args += attrs
         
         out << g.select(args) // name:attrs.name, from:orgs, optionKey:'uid', optionValue:'name', value:attrs.value
      }
   }
   
   /**
    * Admins can assign any role.
    * OrgAdmins can assig OrgAdmins and OrgStaff.
    */
   def selectWithRolesICanAssign = { attrs, body ->
      
      def loggedInUser = springSecurityService.currentUser
      if(loggedInUser)
      {
         def roles = Role.list()
         
         def args = [:]
         args.name = attrs.name
         args.from = roles.authority
         args.class = attrs.class ?: '' // allows set style from outside
         
         if (attrs.user_values)
         {
            // used for the edit, roles are saved in the user, we get them from there
            args.value = attrs.user_values
         }
         else
         {
            // values from params, used when the create fails, ex. when no organization is selected, because the roles are not saved to the user when that happens
            args.value = attrs.value
         }
         
         if (attrs.multiple)
         {
            args.multiple = 'true'
            args.size = 5
         }
         
         if (loggedInUser.authoritiesContains('ROLE_ORG_MANAGER'))
         {
            //roles.removeAll { it.authority == 'ROLE_ADMIN' } // all roles minus admin, removeAll modifies the collection
            //args.from = roles
            args.from.removeElement('ROLE_ADMIN')
         }
         else if (!loggedInUser.authoritiesContains('ROLE_ADMIN'))
         {
            // non admins can't assign any roles
            return
         }
         
         out << g.select(args)
      }
   }
}

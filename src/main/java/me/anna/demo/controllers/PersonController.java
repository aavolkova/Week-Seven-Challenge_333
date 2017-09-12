package me.anna.demo.controllers;
import me.anna.demo.models.*;
import me.anna.demo.repositories.*;
import me.anna.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Controller
//@RequestMapping("/persons")
public class PersonController {
    @Autowired
    PersonRepository personRepository;
    @Autowired
    EducationRepository educationRepository;
    @Autowired
    EmploymentRepository employmentRepository;
    @Autowired
    SkillsRepository skillsRepository;


    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RoleRepo roleRepo;
    @Autowired
    private UserService userService;




    /////////////////////////////////////////////////////////////////
    // ===== Special Route: Need to go to this route just ones,
    // ===== before the first registration of a first user of the App
    // ===== Set Up the Database: create two Roles:=====
    /////////////////////////////////////////////////////////////////
    @RequestMapping("/createRoles")
    public @ResponseBody String createRolesInDatabase() {
        Role r1 = new Role();
        r1.setRole("SEEKER");
        roleRepo.save(r1);

        Role r2 = new Role();
        r2.setRole("RECRUITER");
        roleRepo.save(r2);
        return "two roles have been created";
    }




    // ============ Display the Home page ============
    @GetMapping("/")
    public String showIndex(Model model) {
        String myMessage = "Welcome to the Robo Resume Application";
        model.addAttribute("message", myMessage);
        return "index";
    }




    // ============ Display the Login page ============
    @RequestMapping("/login")
//    public String login(Principal p){
    public String login(){
        return "login";
    }




    // ========== SEEKER's registration FORM ==========
    @RequestMapping(value="/registerSeeker", method = RequestMethod.GET)
    public String showRegistrationSeekerPage (Model model){
        model.addAttribute("user", new Person());
        return "registrationS";
    }
    @RequestMapping(value="/registerSeeker", method = RequestMethod.POST)
    public String processRegistrationSeekerPage (@Valid @ModelAttribute("user") Person person,
                                                 BindingResult result, Model model)
    {
        if (result.hasErrors()){
            return "registrationS";
        } else{
            userService.saveSeeker(person);
            model.addAttribute("message", "User Account Successfully Created");
        }
        return "index";
    }




    // ========= RECRUITER's registration FORM =========
    @RequestMapping(value="/registerRecruiter", method = RequestMethod.GET)
    public String showRegistrationRecruiterPage (Model model){
        model.addAttribute("user", new Person());
        return "registrationR";
    }
    @RequestMapping(value="/registerRecruiter", method = RequestMethod.POST)
    public String processRegistrationRecruiterPage (@Valid @ModelAttribute("user") Person person,
                                                    BindingResult result, Model model) {
        if (result.hasErrors()){
            return "registrationR";
        } else{
            userService.saveRecruiter(person);
            model.addAttribute("message", "User Account Successfully Created");
        }
        return "index";
    }




    // == This page is sheared by both SEEKER and RECRUITER ==
    // ========== Welcome Page / Secure Home Page ============
    @RequestMapping("/welcome")
    public String welcome(Model model, Principal p)
    {
        System.out.println("welcome page: p username:"+ p.getName());
        long countAllSkills = skillsRepository.count();
        model.addAttribute("gotskills", countAllSkills);
        model.addAttribute("skillsList", skillsRepository.findAll());

        Person myPerson = personRepository.findByUsername(p.getName());
        model.addAttribute("currentPerson", myPerson );

        Person Seeker = personRepository.findById(new Long(2));

        Collection <Skills> myPersonSkills = myPerson.getSkillsWithRating();
        for(Skills s : myPersonSkills){

            System.out.println("Skill Title:"+ s.getSkillTitle());
            System.out.println("Skill Id:"+ s.getId());
        }

        //++++++++++++++++++++++ SEEKER ++++++++++++++++++++++++++++++
        ///++++++++++ JOB NOTIFICATIONS FOR Job's SEEKER: ++++++++++++
        long countMatchedJobs = 0;
        List<Job> listOfJobsWithMatchedSkills = jobRepository.findAllBySkillsWithRatingIn(myPerson.getSkillsWithRating());

        for (Job jobs : listOfJobsWithMatchedSkills) {

            System.out.println("Matched Skill Title: " + jobs.getTitle());
            countMatchedJobs += 1;
            //  System.out.println("Matched Skill Title: " + oneSkillMatchedTitle);
        }

        if(countMatchedJobs >= 1){
            model.addAttribute("collectionOfAllMatchedJobs", listOfJobsWithMatchedSkills);
        }

        if(countMatchedJobs == 0){
            listOfJobsWithMatchedSkills = new ArrayList<>();
            model.addAttribute("collectionOfAllMatchedJobs", listOfJobsWithMatchedSkills);
        }

        if(countAllSkills == 0){
            listOfJobsWithMatchedSkills = new ArrayList<>();
            model.addAttribute("collectionOfAllMatchedJobs", listOfJobsWithMatchedSkills);
        }

        return "welcome";
    }


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    // ================== DISPLAY THE FINAL SEEKER RESUME ================
    // ========== Display Person's info saved to the database ============
    // ======== Person can only have one role? and unique username =======
    @GetMapping("/displayPersonAllInfo")
    public String showAllPersonsInfo(Model model, Principal principal)
    {
    //  System.out.println("displayPersonAllInfo page: p.getname:"+ principal.getName());
        Person myPerson = personRepository.findByUsername(principal.getName());
    //  System.out.println("p.getname:"+ principal.getName());
        model.addAttribute("person", myPerson );

        long countAllSkills = skillsRepository.count();
        model.addAttribute("gotskills", countAllSkills);

        return "displayPersonAllInfo";
    }



    // ========== DISPLAY RECRUITER ALL POSTED JOBS ===========
    @GetMapping("/displayRecruiterAllJobPosts")
    public String showAllJobsForThisRecruiter(Model model, Principal principal)
    {
        Person myPerson = personRepository.findByUsername(principal.getName());
        //+++++++ BOTH WAYS WORK !!!!!!! +++++++++
        //Way1:
        Iterable <Job> jobslist = myPerson.getJobs();
        model.addAttribute("alljobs", jobslist);
        //Way2:
        //  Iterable <Job> joblist = jobRepository.findAllByPersonId(myPerson.getId());
        //  model.addAttribute("alljob", joblist);
        model.addAttribute("person", myPerson );
        return "displayRecruiterAllJobPosts";
    }



    //===================== See Job Details ====================
    @GetMapping("/jobDetails/{id}")  //Job id
    public String seeJobDetails(@PathVariable("id") long id, Model model,
                                Principal principal)
    {
        Job job = jobRepository.findOne(id);
        model.addAttribute("job", job);

        Iterable <Skills> skillsList = job.getSkillsWithRating();
        model.addAttribute("skillsWithRating", skillsList);
//        Person myPerson = personRepository.findByUsername(principal.getName());
//        model.addAttribute("person", myPerson );

        return "/jobDetails";
    }




//OBSOLETE:
//    //=========== List ALL Persons/Users ==============
//    @GetMapping("/allPersons")
//    public String showAllPersons(Model model, Principal p)
//    {
//        Iterable <Person> personslist = personRepository.findAll();
//        model.addAttribute("allpersons", personslist);
//        return "allPersons";
// == }





    // ============== Add Education ===============
    // Allow user to enter Educational Achievements
    @GetMapping("/enterEducation/{id}") //Person's ID
    public String addEducation(@PathVariable("id") long id, Model model)
    {
        Person p = personRepository.findOne(id);
        model.addAttribute("newPerson", p);

        Education e = new Education();
//        ????????????????????
//        e.setPerson(p);
        p.addEducation(e);

        model.addAttribute("newEducation", e);
        return "enterEducation";
    }
    // Validate entered Educational Achievement and if it is valid display the result
    @PostMapping("/enterEducation")
    public String postEducation(@Valid @ModelAttribute("newEducation") Education education,
                                BindingResult bindingResult, Model model)
    {
        if(bindingResult.hasErrors()){return "enterEducation";}

        model.addAttribute("newEducation", education);

        long personId = education.getPerson().getId();
        Person p = personRepository.findOne(personId);
        model.addAttribute("personObject", p);

        educationRepository.save(education);
        return "resultEducation";
    }





    // ============== Add Employment ==============
    // Allow user to enter Employment
    @GetMapping("/enterEmployment/{id}") //Person's ID
    public String addEmployment(@PathVariable("id") long id, Model model)
    {
        Person p = personRepository.findOne(id);
        model.addAttribute("newPerson", p);

        Employment w = new Employment();
//        ????????????????????
//        w.setPerson(p);
        p.addEmployment(w);

        model.addAttribute("newEmployment", w);
        return "enterEmployment";
    }
    // Validate entered work experience and if it is valid display the result
    @PostMapping("/enterEmployment")
    public String postEmployment(@Valid @ModelAttribute("newEmployment") Employment employment,
                                 BindingResult bindingResult, Model model)
    {
        if(bindingResult.hasErrors()){return "enterEmployment";}

        // === Allow user to leave the end date empty (assume he/she is still employed)
        if(employment.getEndDate() == null){
            employment.setEndDate(LocalDate.now());
        }
        //=== If dates entered, do not accept the end date before the start date
        else if(employment.getEndDate().compareTo(employment.getStartDate())< 0){
            return "enterEmployment";
        }
        model.addAttribute("newEmployment", employment);

        long personId = employment.getPerson().getId();
        Person p = personRepository.findOne(personId);
        model.addAttribute("personObject", p);

        employmentRepository.save(employment);
        return "resultEmployment";
    }





    //======================== Create a New Skill ======================
    //====== ADD SKILL TO DB (not connected with Job or Person yet =====
    @GetMapping("/addskill")
    public String addMovie(Model model)
    {
        Skills skill = new Skills();
        model.addAttribute("skill", skill);
        return "addskill";
    }
    @PostMapping("/addskill")
    public String saveMovie(@Valid @ModelAttribute("skill") Skills skill,
                            BindingResult bindingResult, Model model)
    {
        skillsRepository.save(skill);
        return "redirect:/welcome";
    }




    ///////////////////////////////////////////////////////////////////////
    //====================== ADD SKILL TO PERSON ==========================
    @GetMapping("/addskillstoperson")
    public String addSkill(Model model, Principal principal)
    {
        Person myPerson = personRepository.findByUsername(principal.getName());
        model.addAttribute("currentPerson", myPerson);
//        model.addAttribute("p", personRepository.findByUsername(p.getName()));

//        Collection <Skills> myPersonSkills = myPerson.getSkillsWithRating();

//        Skills sPerson = skillsRepository.findBySkillTitleIn(myPersonSkills);
//        System.out.println("Skill sPerson Title "+s.getSkillTitle());

        // Need to do:
        // Need to check here if the person already has this skill and if not add it to list

        model.addAttribute("skillList",skillsRepository.findAll());
        return "personaddskill";
    }

    @PostMapping("/addskillstoperson")
    public String addSkillsToPerson(@RequestParam(value = "skills") long skills,
                                    Model model, Principal principal)
    {
        Person myPerson = personRepository.findByUsername(principal.getName());
        model.addAttribute("currentPerson", myPerson );
        System.out.println("Skill ID: "+skills);
        System.out.println("Person username "+ principal.getName());
        System.out.println("Person Last name "+ myPerson.getLastName());

        // Skill chosen from drop down menu
        Skills s = skillsRepository.findOne(new Long(skills));
        System.out.println("Skill Title "+s.getSkillTitle());

        myPerson.addSkill(s);
        personRepository.save(myPerson);

//        model.addAttribute("personList", personRepository.findAll());
        model.addAttribute("skillList",skillsRepository.findAll());  //???????????????
//        model.addAttribute("movieList",movieRepository.findAll());

//        model.addAttribute("myPerson", movieRepository.findAll());
        return "redirect:/welcome";
    }
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    ///////////////////////////////////////////////////////////////////////
    //====================== REMOVE SKILL FROM PERSON =====================
    ///////////////////////////////////////////////////////////////////////
    @RequestMapping("/deleteSkills/{id}")
    public String delSkills(@PathVariable("id") long id, Principal principal)
    {
        Skills oneSkill = skillsRepository.findOne(id);
        Person myPerson = personRepository.findByUsername(principal.getName());

        myPerson.getSkillsWithRating().remove(oneSkill);
        personRepository.save(myPerson);

        return "redirect:/displayPersonAllInfo";
    }






    /////////////////////////////////////////////////////////////////
    //======= I Made This Option Available only for SEEKER //////////
    //=================== Update Personal Info ======================
    /////////////////////////////////////////////////////////////////
    @GetMapping("/updatePerson/{id}")
    public String updatePerson(@PathVariable("id") long id, Model model)
    {
        Person p = personRepository.findOne(id);
        model.addAttribute("user", p);
        return "registrationS";
    }
//===== Obsolete, I used this option for the Weekly Challenge 6 =====
//    //========= DELETE ENTIRE Person and his/her Resume  ==========
//    @GetMapping("/deletePerson/{id}")
//    public String deletePerson(@PathVariable("id") long id)
//    {
//        personRepository.delete(id);
//        return "redirect:/allPersons";
//    }






    //================= Update, Delete Education ===================
    //===================== UPDATE Education =======================
    @GetMapping("/updateEducation/{id}")  //Education id
    public String updateEducation(@PathVariable("id") long id, Model model)
    {
        model.addAttribute("newEducation", educationRepository.findOne(id));
        return "enterEducation";
    }

    //==================== DELETE Education ========================
    @RequestMapping("/deleteEducation/{id}")
    public String delEducation(@PathVariable("id") long id)
    {
        Education oneEducation= educationRepository.findOne(id);
        long personToGoTo = oneEducation.getPerson().getId();

        // you MUST first remove the Education from the Set of educationalAchievements
        // for their person, then you can delete the education
        // Remove Education from person
        educationRepository.findOne(id).getPerson().removeEducation(oneEducation);

        // delete Education from the education table
        educationRepository.delete(id);
//        return "redirect:/displayPersonAllInfo/" + personToGoTo;

        return "redirect:/displayPersonAllInfo";
    }




    //================ Update, Delete Employment ===================
    //==================== UPDATE Employment =======================
    @GetMapping("/updateEmployment/{id}")  //Education id
    public String updateEmployment(@PathVariable("id") long id, Model model)
    {
        Employment e = employmentRepository.findOne(id);
        model.addAttribute("newEmployment", e);
        // The same in one line:
        // model.addAttribute("newEmployment", employmentRepository.findOne(id));
        return "enterEmployment";
    }

    //==================== Delete Employment =======================
    @RequestMapping("/deleteEmployment/{id}")
    public String delEmployment(@PathVariable("id") long id)
    {
        Employment oneEmployment= employmentRepository.findOne(id);
        long personToGoTo = oneEmployment.getPerson().getId();

        // you MUST first remove the Employment from the Set of workExperience
        // for their person, then you can delete the skill
        // Remove Employment from person
        employmentRepository.findOne(id).getPerson().removeEmployment(oneEmployment);

        // delete Employment from the employment table
        employmentRepository.delete(id);
//        return "redirect:/displayPersonAllInfo/" + personToGoTo;
        return "redirect:/displayPersonAllInfo";
    }


    ////////////////////////////////////////////////////////////////
    //====================== CREATE NEW JOB  =====================
    ////////////////////////////////////////////////////////////////
    //===== This job is not connecting to a Person or Job yet ======
    // ========================= Add Job ===========================
    @GetMapping("/addjob")
    public String addJob(Model model, Principal principal)
    {
//        Person p = personRepository.findOne(id);
//        model.addAttribute("newPerson", p);  /////////??????
        Person myPerson = personRepository.findByUsername(principal.getName());
        model.addAttribute("currentPerson", myPerson );

        Job j = new Job();
        myPerson.addJob(j);

        model.addAttribute("newJob", j);
        return "addjob";
    }
    // Validate entered skill info and if it is valid display the result
    @PostMapping("/addjob")
    public String postJob(@Valid @ModelAttribute("newJob") Job job,
                          BindingResult bindingResult, Model model,
                          Principal principal)
    {
        if(bindingResult.hasErrors()){return "addjob";}

        model.addAttribute("job", job);
        Person myPerson = personRepository.findByUsername(principal.getName());
//        model.addAttribute("currentPerson", myPerson );
        System.out.println("Recruiter last name "+ myPerson.getLastName());
        model.addAttribute("personObject", myPerson);

        jobRepository.save(job);


        // Addition to disable add skill to job button:
        long countAllSkills = skillsRepository.count();
        model.addAttribute("gotskills", countAllSkills);

        return "resultJob";
    }

    ///////////////////////////////////////////////////////////////////////
    //========================= ADD SKILL TO JOB ==========================
    ///////////////////////////////////////////////////////////////////////
    //========================= ADD SKILL TO JOB ==========================
    @GetMapping("/addskilltojob/{id}")// This is JOB ID
    public String addSkillToJob(@PathVariable("id") long id, Model model,
                                Principal principal)
    {
        Person myPerson = personRepository.findByUsername(principal.getName());
        Job myJob = jobRepository.findOne(new Long(id));

        model.addAttribute("currentPerson", myPerson);
        model.addAttribute("currentJob", myJob);
        model.addAttribute("skillList",skillsRepository.findAll());

        System.out.println("GetMapping: Recruiter last name "+ myPerson.getLastName());
        System.out.println("GetMapping: Job title: "+ myJob.getTitle());

        return "jobaddskill";
    }
    // Attach existed Skill to Job
    @PostMapping("/addskilltojob")///{id}")// This is JOB ID
    public String processAddSkillsToJob(//@PathVariable("id") long id, // This is JOB ID
                                        @RequestParam(value = "skills") long skills,
                                        @RequestParam(value = "currentJob") long currentJob,
                                    //  @ModelAttribute("currentJob") Job myJob,
                                    Model model, Principal principal)
    {
        Person myPerson = personRepository.findByUsername(principal.getName());
//        Job myJob = jobRepository.findOne(new Long(id));
        Job j = jobRepository.findOne(new Long(currentJob));

        model.addAttribute("currentPerson", myPerson );
        model.addAttribute("currentJob", j);

        System.out.println("Person username "+ principal.getName());
        System.out.println("Person Last name "+ myPerson.getLastName());
        System.out.println("Job title: "+ j.getTitle());



        Skills s = skillsRepository.findOne(new Long(skills));
//        System.out.println("Skill Title: "+skills);
        System.out.println("Skill Title: "+s.getSkillTitle());

        j.addSkill(s);
        jobRepository.save(j);
//        model.addAttribute("personList", personRepository.findAll());
        model.addAttribute("skillList",skillsRepository.findAll());

        long jobToGoTo = j.getId();
//        return  "redirect:/jobDetails/{id}";  // This is JOB ID
//        return "redirect:/jobDetails/" + jobToGoTo;
        return "redirect:/welcome";
    } //////////////////////////////////////////////////////////////////////






    //////////////////////////////////////////////////////////////////////
    // =========================== JOB SEARCH ============================
    @GetMapping("/jobsearch")
    public String searchJob()
    {
        return "jobsearch";
    }
    //////////////////////////////////////////////////////////////////////
    // =========================== JOB SEARCH ============================
    ////////////////////////////// BY COMPANY ////////////////////////////
    @PostMapping("/jobsearchbycompany")
    public String processSearchJobByCompany(@RequestParam(value = "companyname") String companyname,
                                            Model model)
    {
//        Iterable <Job> jobsList = jobRepository.findAllByEmployerLike(companyname);

        Iterable <Job> jobsList = jobRepository.findAllByEmployerContains(companyname);
        System.out.println(companyname);
        for(Job j: jobsList){
            System.out.println("Job Company: " + j.getEmployer());
        }
        model.addAttribute("allJobsMatched", jobsList);
        return "jobsearchresult";
    }

    //////////////////////////////////////////////////////////////////////
    // =========================== JOB SEARCH ============================
    /////////////////////////////// BY TITLE /////////////////////////////
    @PostMapping("/jobsearchbytitle")
    public String processSearchJobByTitle(@RequestParam(value = "positiontitle") String positiontitle,
                                          Model model)
    {
        System.out.println(positiontitle);

        Iterable <Job> jobsList = jobRepository.findAllByTitleContains(positiontitle);
        for(Job j: jobsList){
            System.out.println("Job Title: " + j.getTitle());
        }
        model.addAttribute("allJobsMatched", jobsList);
        return "jobsearchresult";
    }


    //////////////////////////////////////////////////////////////////////
    // ========================= PEOPLE SEARCH ===========================
    @GetMapping("/peoplesearch")
    public String searchPeople()
    {
        return "peoplesearch";
    }
    //////////////////////////////////////////////////////////////////////
    // ========================= PEOPLE SEARCH ===========================
    ////////////////////////////// BY SCHOOL /////////////////////////////
    @PostMapping("/peoplesearchbyschool")
    public String processSearchPeopleBySchool(@RequestParam(value = "schoolname") String schoolname,
                                              Model model)
    {
        // Iterable <Job> jobsList = jobRepository.findAllByEmployerLike(companyname);
        System.out.println(schoolname);
//        List<Person> searchPersonList= personRepository.findAllByEducationalAchievementsIsContaining(schoolname);

       Set<Education> edu= educationRepository.findByEducationalInstitution(schoolname);
        List<Person> searchPersonList= personRepository.findAllByEducationalAchievementsIn(edu);

//      findAllByByEducationalAchievements_EducationalInstitution(schoolname);
        model.addAttribute("searchPersonList", searchPersonList);

//        Iterable <Job> jobsList = jobRepository.findAllByEmployerContains(schoolname);
//        System.out.println(schoolname);
//        for(Job j: jobsList){
//            System.out.println("school name: " + j.getEmployer());
//        }
//        model.addAttribute("allJobsMatched", jobsList);
//
        return "peoplesearchresult";
    }
    //////////////////////////////////////////////////////////////////////
    // ========================= PEOPLE SEARCH ===========================
    ///////////////////////////// BY FUlL NAME ///////////////////////////
    //== Tried to make a search ignore case: without success: need to do
    @PostMapping("/peoplesearchbyname")
    public String processSearchPeopleByName(@RequestParam(value = "searchFirstName") String searchFirstName,
                                            @RequestParam(value = "searchLastName") String searchLastName,
                                            Model model)
    {
//        Iterable <Job> jobsList = jobRepository.findAllByEmployerLike(companyname);
        System.out.println("person name: " + searchFirstName + " " + searchLastName);
//        Iterable <Job> jobsList = jobRepository.findAllByEmployerContains(personname);
//        for(Job j: jobsList){
//            System.out.println("Job Title: " + j.getTitle());
//        }
//        model.addAttribute("allJobsMatched", jobsList);

        //  It works:
        Iterable<Person> searchPersonList = personRepository.findAllByFirstNameAndLastName(searchFirstName, searchLastName);
        for(Person p: searchPersonList){
            System.out.println("Find person's email: " + p.getEmailAddress());
        }

        // Then tried this: it doesn't work:
//        Iterable<Person> personList = personRepository.findAllByLastNameAndFirstNameAllIgnoreCase(searchFirstName, searchLastName);
        // Then try this: it doesn't work:
//        Iterable<Person> personList = personRepository.findAllByLastNameAndFirstNameAllIgnoreCase(searchFirstName, searchLastName);
        model.addAttribute("searchPersonList", searchPersonList);
        return "peoplesearchresult";
    }



    ////////////////////////////////////////////////////////////////////////////////////////
    //==/== When Recruiter find a Person, He will be able to see the Person's Resume =====//
    // Need it to display this info as details about the find person. Display for Recruiter!
    @GetMapping("/displayPersonInfo/{id}")
    public String showAllPersonsInfo(@PathVariable("id") long id, Model model){

        Person myPerson = personRepository.findOne(id);
        model.addAttribute("person", myPerson );

        return "displayPerson";
    }




}

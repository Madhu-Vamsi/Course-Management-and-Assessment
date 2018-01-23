import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application {
    Scanner scan;
    String userId = "";
    List<String> userCourses;
    List<Integer> userHws;
    int role = -1;
    Connection connection;
    static final String jdbcURL = "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01";

    public Application(Scanner scan) {
        this.scan = scan;
    }

    public void prompt(String in) {
        System.out.println(in);
    }

    public void instructorHandler() throws SQLException {
        int choice = this.viewInstructorMenu();
        switch (choice) {
        case 1:
            this.viewInstructorProfile();
            return;
        case 2:
            this.viewOrAddCourse();
            return;
        case 3:
            this.scan.nextLine();
            this.prompt("Enter CourseID");
            String courseID = this.scan.nextLine();
            int option = -1;
            this.prompt("1. Enroll a student 2. Drop a student");
            option = scan.nextInt();
            if (option == 0)
                this.enroll(courseID);
            else
                this.drop(courseID);
            this.instructorHandler();
            return;
        case 4:
            this.searchOrAddQuestions();
            this.instructorHandler();
            return;
        case 5:
            this.createQuestionBank();
            this.instructorHandler();
            return;
        case 6:
            Application.main(null);
            return;
        default:
            this.prompt("Invalid Input");
            this.instructorHandler();
            break;
        }
        if (choice == 0)
            this.instructorHandler();
        return;
    }

    public void viewAllQuestions() {
        String sql = "Select * from Question";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                this.prompt("ID: " + rs.getInt(1) + "\tDescription: " + rs.getString(2) + "\tDifficulty: "
                        + rs.getString(3));
            }
        } catch (SQLException e) {
            this.prompt("Failed to show all Questions " + e);
        }
        return;
    }

    public void utilityViewTopicQuestions(int topicid) {
        try {
            String sql = "Select description, difficulty from question q, "
                    + "question_topic qt where qt.QID = q.id and qt.TID = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, topicid);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                this.prompt("Question: " + rs.getString(1) + "\tDifficulty: " + rs.getInt(2));
            }
        } catch (Exception e) {
            this.prompt("View Questions for a specific Topic Failed: " + e);
        }
    }

    public void viewTopics() {
        String sql = "Select * from topic";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs1 = statement.executeQuery();
            while (rs1.next()) {
                this.prompt("ID: " + rs1.getInt(1) + "\tTopic: " + rs1.getString(2));
            }
        } catch (SQLException e) {
            this.prompt("View Questions for a specific Topic Failed: " + e);
        }
    }

    public void viewTopicQuestions() {
        this.viewTopics();
        this.scan.nextLine();
        this.prompt("Enter Topic ID");
        int topicid = this.scan.nextInt();
        this.utilityViewTopicQuestions(topicid);
    }

    public void searchOrAddQuestions() {
        this.scan.nextLine();
        this.prompt("1: View All Questions 2: View Questions: Specific Topic 3: Add a new Question 0: Back");
        int choice = this.scan.nextInt();
        switch (choice) {
        case 1:
            this.viewAllQuestions();
            return;
        case 2:
            this.viewTopicQuestions();
            return;
        case 3:
            this.addQuestion();
            return;
        case 0:
            return;

        }
    }

    public void addParametricQuestion() {
        this.scan.nextLine();
        System.out.println("Enter Question Text with parameters enclosed inside < >");
        String text = scan.nextLine();
        Matcher m = Pattern.compile("\\<(.*?)\\>").matcher(text);
        this.prompt("Enter Difficulty level (1-6)");
        int level = scan.nextInt();
        this.viewTopics();
        this.prompt("Enter the topic ID");
        int topicid = scan.nextInt();
        String sql = "Select QUESTION_SEQ.NEXT_VAL from dual";
        int qid = -1;
        try {
            /*
             * try { PreparedStatement statement =
             * connection.prepareStatement(sql); ResultSet rs =
             * statement.executeQuery(); if(rs.next()) { qid = rs.getInt(1); }
             * }catch(SQLException e) {
             * this.prompt("Next question id is failed to return"+e); }
             */

            String insert1, insert2, insert3, insert4;
            PreparedStatement st1, st2, st3, st4;
            insert1 = "INSERT INTO QUESTION (description, difficulty) VALUES (?, ?)";
            st1 = connection.prepareStatement(insert1);
            st1.setString(1, text);
            st1.setInt(2, level);
            insert2 = "INSERT INTO question_topic VALUES(QUESTION_SEQ.currval, ?)";
            st2 = connection.prepareStatement(insert2);
            // st2.setInt(1, qid);
            st2.setInt(1, topicid);
            insert3 = "INSERT INTO parameterized_question VALUES(QUESTION_SEQ.currval)";
            st3 = connection.prepareStatement(insert3);
            // st3.setInt(1, qid);
            try {
                st1.executeQuery();
            } catch (SQLException e) {
                this.prompt("Failed to add Question: Parent" + e);
            }
            try {
                st2.executeQuery();
            } catch (SQLException e) {
                this.prompt("Failed to add Topic to question" + e);
            }
            try {
                st3.executeQuery();
            } catch (SQLException e) {
                this.prompt("Failed to add Question: Subtype Parametric" + e);
            }
            try {
                while (m.find()) {
                    insert4 = "INSERT INTO parameter (qid, variable) VALUES(QUESTION_SEQ.currval, ?)";
                    st4 = connection.prepareStatement(insert4);
                    // st4.setInt(1, qid);
                    st4.setString(1, m.group(1));
                    st4.executeQuery();
                }
            } catch (Exception e) {
                this.prompt("Failed to add paramters" + e);
            }
        } catch (SQLException e) {
            this.prompt("Failed to add Parametric Question");
        }
    }

    public void viewAllParameterizedQuestions() {
        String sql = "select q.ID,q.DESCRIPTION,q.DIFFICULTY from parameterized_question pq left join question q on pq.QID = q.ID";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            this.prompt("ID\tDescription\tDifficulty");
            while (rs.next()) {
                this.prompt(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3));
            }
        } catch (SQLException e) {
            this.prompt("Failed to view all param questions" + e);
        }
    }

    public void parametricToFixed() {
        this.viewAllParameterizedQuestions();
        this.prompt("Enter the ID of the Question");
        int parid = this.scan.nextInt();
        String sql = "Select variable from parameter where qid=?";
        String sql2 = "select q.ID,q.DESCRIPTION,q.DIFFICULTY from parameterized_question pq left join question q on pq.QID = q.ID where q.ID=?";
        String sql3 = "select qt.TID from QUESTION_TOPIC qt left join "
                + "parameterized_question pq on pq.QID = qt.QID where pq.QID=?";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            PreparedStatement st2 = connection.prepareStatement(sql2);
            PreparedStatement st3 = connection.prepareStatement(sql3);
            st.setInt(1, parid);
            st2.setInt(1, parid);
            st3.setInt(1, parid);
            ResultSet rs = st.executeQuery();
            ResultSet rs2 = st2.executeQuery();
            ResultSet rs3 = st3.executeQuery();
            String text = "";
            if (rs2.next())
                text = rs2.getString(2);
            int difficulty = 1;
            if (rs2.next())
                difficulty = rs2.getInt(3);
            int topicid = 1;
            if (rs3.next())
                topicid = rs3.getInt(1);
            this.prompt("Enter parameter values as asked in order");
            this.scan.nextLine();
            while (rs.next()) {
                this.prompt(rs.getString(1));
                String val = this.scan.nextLine();
                text = text.replaceAll(rs.getString(1), rs.getString(1) + ":" + val);
            }
            this.utilityFixedQuestion(text, difficulty, topicid, parid);

        } catch (SQLException e) {
            this.prompt("Failed to read parameters" + e);
        }
    }

    public void utilityFixedQuestion(String text, int difficulty, int topicid, int parid) {
        int choice;
        String sql1 = "INSERT INTO QUESTION (description, difficulty) VALUES (?, ?)";
        String sql2 = "INSERT INTO question_topic VALUES(QUESTION_SEQ.currval, ?)";
        // this.scan.nextLine();
        this.prompt("Enter Hint");
        String hint = this.scan.nextLine();
        this.prompt("Enter detailed explaination");
        String detailExp = this.scan.nextLine();
        String sql3 = "INSERT INTO fixed_question VALUES(QUESTION_SEQ.currval, ?, ?, ?)";
        try {
            PreparedStatement st1 = connection.prepareStatement(sql1);
            PreparedStatement st2 = connection.prepareStatement(sql2);
            PreparedStatement st3 = connection.prepareStatement(sql3);
            st1.setString(1, text);
            st1.setInt(2, difficulty);
            st2.setInt(1, topicid);
            st3.setString(1, hint);
            st3.setString(2, detailExp);
            if (parid == 0)
                st3.setNull(3, 0);
            else
                st3.setInt(3, parid);
            st1.executeQuery();
            st2.executeQuery();
            st3.executeQuery();
            this.prompt("Enter Correct and Incorrect Answers");
            while (true) {
                // this.scan.nextLine();
                this.prompt("1: Correct 2: Incorrect 0:Break");
                choice = this.scan.nextInt();
                this.scan.nextLine();
                String sqlans, explain, ans;
                PreparedStatement st4;
                switch (choice) {
                case 0:
                    return;
                case 1:
                    sqlans = "INSERT INTO answer(qid, answer, explanation, iscorrect) VALUES(QUESTION_SEQ.currval,?,?,?)";
                    this.prompt("Enter answer");
                    ans = this.scan.nextLine();
                    this.prompt("Enter Explaination");
                    explain = this.scan.nextLine();
                    st4 = connection.prepareStatement(sqlans);
                    st4.setString(1, ans);
                    st4.setString(2, explain);
                    st4.setInt(3, 1);
                    st4.executeQuery();
                    break;
                case 2:
                    sqlans = "INSERT INTO answer(qid, answer, explanation, iscorrect) VALUES(QUESTION_SEQ.currval,?,?,?)";
                    this.prompt("Enter Incorrect answer");
                    ans = this.scan.nextLine();
                    this.prompt("Enter Explaination");
                    explain = this.scan.nextLine();
                    st4 = connection.prepareStatement(sqlans);
                    st4.setString(1, ans);
                    st4.setString(2, explain);
                    st4.setInt(3, 0);
                    st4.executeQuery();
                    break;
                default:
                    break;
                }
            }

        } catch (Exception e) {
            this.prompt(e.toString());
        }
    }

    public void addFixedQuestion() {
        this.scan.nextLine();
        this.prompt("1:Has Parent Parametric Question 2: No parent 0:Back");
        int choice = this.scan.nextInt();
        if (choice == 0)
            return;
        else if (choice == 1) {
            this.parametricToFixed();
        } else {
            this.scan.nextLine();
            this.prompt("Enter Question Text");
            String text = this.scan.nextLine();

            this.prompt("Enter Difficulty level (1-6)");
            int level = scan.nextInt();
            this.viewTopics();
            this.prompt("Enter the topic ID");
            int topicid = scan.nextInt();
            this.utilityFixedQuestion(text, level, topicid, 0);
        }
        return;
    }

    public void addQuestion() {
        this.scan.nextLine();
        this.prompt("1: Fixed Question 2: Parametric Question 0:Back");
        int fp = scan.nextInt();
        switch (fp) {
        case 0:
            this.searchOrAddQuestions();
            return;
        case 1:
            this.addFixedQuestion();
            return;
        case 2:
            this.addParametricQuestion();
            return;
        default:
            return;
        }
    }

    public void enrollByTA(String courseID) {
        this.scan.nextLine();
        this.prompt("Enter Student ID");
        String stuID = scan.nextLine();
        /*
         * this.prompt("Enter Student First Name"); String fname =
         * scan.nextLine(); this.prompt("Enter Student Last Name"); String lname
         * = scan.nextLine();
         */
        // Enroll the student id not there already
        this.prompt("------------------OUTPUT-----------------------\n");
        try {
            String sql = "Select * from enrollment e where e.cid=? and e.sid=? and e.ista=?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, courseID);
            st.setString(2, this.userId);
            st.setInt(3, 1);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                this.prompt("You are not authorized for this course");
                return;
            }
        } catch (SQLException se) {
            this.prompt("Failed to enroll : " + se.toString());
        }
        try {
            String sql1 = "Select * from enrollment where sid=? and cid=?";
            PreparedStatement st1 = connection.prepareStatement(sql1);
            st1.setString(1, stuID);
            st1.setString(2, courseID);
            ResultSet rs = st1.executeQuery();
            if (rs.next()) {
                this.prompt("Student already enrolled as Student/TA");
            } else {
                String sql2 = "INSERT INTO ENROLLMENT VALUES(?, ?, ?)";
                PreparedStatement st2 = connection.prepareStatement(sql2);
                st2.setString(1, stuID);
                st2.setString(2, courseID);
                st2.setInt(3, 0);
                st2.executeQuery();
                this.prompt("Successfully enrolled as student : " + stuID);
            }

        } catch (SQLException e) {
            this.prompt("Failed to enroll: " + e.toString());
        }
        this.prompt("---------------------------------------------------");
        return;
    }

    public void enroll(String courseID) {
        this.scan.nextLine();
        this.prompt("Enter Student ID");
        String stuID = scan.nextLine();
        /*
         * this.prompt("Enter Student First Name"); String fname =
         * scan.nextLine(); this.prompt("Enter Student Last Name"); String lname
         * = scan.nextLine();
         */
        // Enroll the student id not there already
        this.prompt("------------------OUTPUT-----------------------\n");
        try {
            String sql = "Select * from course where id=? and pid=?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, courseID);
            st.setString(2, this.userId);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                this.prompt("You are not authorized for this course");
                return;
            }
        } catch (SQLException se) {
            this.prompt("Failed to enroll : " + se.toString());
        }
        try {
            String sql1 = "Select * from enrollment where sid=? and cid=?";
            PreparedStatement st1 = connection.prepareStatement(sql1);
            st1.setString(1, stuID);
            st1.setString(2, courseID);
            ResultSet rs = st1.executeQuery();
            if (rs.next()) {
                this.prompt("Student already enrolled as Student/TA");
            } else {
                String sql2 = "INSERT INTO ENROLLMENT VALUES(?, ?, ?)";
                PreparedStatement st2 = connection.prepareStatement(sql2);
                st2.setString(1, stuID);
                st2.setString(2, courseID);
                st2.setInt(3, 0);
                st2.executeQuery();
                this.prompt("Successfully enrolled as student : " + stuID);
            }

        } catch (SQLException e) {
            this.prompt("Failed to enroll: " + e.toString());
        }
        this.prompt("---------------------------------------------------");
        return;
    }

    public void dropByTA(String courseID) {
        this.scan.nextLine();
        this.prompt("Enter Student ID");
        String stuID = scan.next();
        /*
         * this.prompt("Enter Student First Name"); String fname =
         * scan.nextLine(); this.prompt("Enter Student Last Name"); String lname
         * = scan.nextLine();
         */
        // Drop the student id if there in the course ID
        this.prompt("------------------OUTPUT-----------------------\n");
        try {
            String sql = "Select * from enrollment e where e.cid=? and e.sid=? and e.ista=?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, courseID);
            st.setString(2, this.userId);
            st.setInt(3, 1);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                this.prompt("You are not authorized for this course");
                return;
            }
        } catch (SQLException se) {
            this.prompt("Failed to drop : " + se.toString());
        }
        try {
            String sql1 = "Select * from enrollment where sid=? and cid=? and isTA=?";
            PreparedStatement st1 = connection.prepareStatement(sql1);
            st1.setString(1, stuID);
            st1.setString(2, courseID);
            st1.setInt(3, 0);
            ResultSet rs = st1.executeQuery();
            if (rs.next()) {
                String sql2 = "DELETE from enrollment where sid=? and cid=?";
                PreparedStatement st2 = connection.prepareStatement(sql2);
                st2.setString(1, stuID);
                st2.setString(2, courseID);
                try {
                    st2.executeQuery();
                    this.prompt("Successfully dropped the student");
                } catch (SQLException s) {
                    this.prompt("Failed to drop the student :" + s.toString());
                }
            } else {
                this.prompt("No such student exists");
            }

        } catch (SQLException e) {
            this.prompt("Failed to drop: " + e.toString());
        }
        this.prompt("---------------------------------------------------");
        return;
    }

    public void drop(String courseID) {
        this.scan.nextLine();
        this.prompt("Enter Student ID");
        String stuID = scan.next();
        /*
         * this.prompt("Enter Student First Name"); String fname =
         * scan.nextLine(); this.prompt("Enter Student Last Name"); String lname
         * = scan.nextLine();
         */
        // Drop the student id if there in the course ID
        this.prompt("------------------OUTPUT-----------------------\n");
        try {
            String sql = "Select * from course where id=? and pid=?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, courseID);
            st.setString(2, this.userId);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                this.prompt("You are not authorized for this course");
                return;
            }
        } catch (SQLException se) {
            this.prompt("Failed to drop : " + se.toString());
        }
        try {
            String sql1 = "Select * from enrollment where sid=? and cid=? and isTA=?";
            PreparedStatement st1 = connection.prepareStatement(sql1);
            st1.setString(1, stuID);
            st1.setString(2, courseID);
            st1.setInt(3, 0);
            ResultSet rs = st1.executeQuery();
            if (rs.next()) {
                String sql2 = "DELETE from enrollment where sid=? and cid=?";
                PreparedStatement st2 = connection.prepareStatement(sql2);
                st2.setString(1, stuID);
                st2.setString(2, courseID);
                try {
                    st2.executeQuery();
                    this.prompt("Successfully dropped the student");
                } catch (SQLException s) {
                    this.prompt("Failed to drop the student :" + s.toString());
                }
            } else {
                this.prompt("No such student exists");
            }

        } catch (SQLException e) {
            this.prompt("Failed to drop: " + e.toString());
        }
        this.prompt("---------------------------------------------------");
        return;
    }

    public int viewInstructorMenu() {
        this.prompt("1.View Profile  2.View/Add courses 3.Enroll/Drop a student ");
        this.prompt("4:Search/Add questions  5. Create Question Bank 6.Logout ");
        while (true) {
            try {
                int choice = scan.nextInt();
                if (choice >= 1 && choice <= 5)
                    return choice;
            } catch (Exception e) {
                this.prompt("Invalid Input");
            }
        }
    }

    // 1
    public void viewInstructorProfile() throws SQLException {
        // Get Instructor Profile
        String first = "";
        String last = "";
        String empid = "";
        try {
            String sql = "SELECT * FROM professor WHERE userid = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, this.userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                first = resultSet.getString("fname");
                last = resultSet.getString("lname");
                empid = this.userId;
                break;
            }

        } catch (SQLException e) {
            this.prompt(e.toString());
        }

        this.prompt("1. First Name : " + first);
        this.prompt("2. Last Name : " + last);
        this.prompt("3. EmployeeID : " + empid);
        this.prompt("Press 0 to get back to Menu");
        int choice = this.scan.nextInt();
        if (choice == 0)
            this.instructorHandler();
        else {
            this.prompt("Wrong input. Exiting");
            return;
        }
    }

    public void viewCourses() {
        // get courses offered by instructor if role = 0
        String courseID = "";
        String cname = "";
        if (role == 0) {
            // show the courses offered by the professor
            this.prompt(this.userId);
            String sql = "SELECT * FROM course WHERE pid = ?";
            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, this.userId);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    this.prompt("CourseID : " + resultSet.getString(1) + "  Name:  " + resultSet.getString(2));
                }
            } catch (Exception e) {
                this.prompt("Failed to View Professor Courses" + e);
            }
        } else {
            // show the courses enrolled by the student
        }
    }

    // 2
    public void viewOrAddCourse() throws SQLException {
        this.viewCourses();
        this.prompt("1:View a Course 2: Add Course 0: Back to Menu");
        int choice = this.scan.nextInt();
        String courseID = "";
        switch (choice) {
        case 0:
            this.instructorHandler();
            return;
        case 1:
            this.prompt("Enter courseID");
            this.scan.nextLine();
            courseID = this.scan.nextLine();
            this.viewCourse(courseID);
            this.instructorHandler();
            return;
        case 2:
            this.addCourse();
            return;
        default:
            this.prompt("Invalid Input. Exiting");
            return;
        }
    }

    public void viewExercises(String courseID) throws SQLException {
        String sql = "Select E.id, E.name from course_exercise CE, exercise E where CE.eid=E.id and CE.cid=?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseID);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            this.prompt("ID: " + resultSet.getString(1) + "\tExercise Details : " + resultSet.getString(2));
        }
    }

    public void viewCourse(String courseID) throws SQLException {
        String courseName = "";
        String startDate = "";
        String endDate = "";
        // Print course Details
        String sql = "SELECT * FROM course WHERE ID = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, courseID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                this.prompt("Course Name: " + resultSet.getString(2));
                this.prompt("Start Date: " + resultSet.getString(4));
                this.prompt("EndDate: " + resultSet.getString(5));
            }
        } catch (SQLException e) {
            this.prompt("Failed to View Specific Professor Course : " + e);
        }
        this.prompt("1: View Exercises 2: Add Exercises");
        this.prompt("3: View TA 4: Add TA");
        this.prompt("5. Enroll 6.Drop a student");
        this.prompt("7.View QuestionBank 8. Add Question Bank");
        this.prompt("0: Back to Previous Menu");
        int choice = this.scan.nextInt();
        switch (choice) {
        case 0:
            return;
        case 1:
            this.viewExercises(courseID);
            this.prompt("Enter Exercise ID to view or 0: Return to previous Menu");
            int eId = this.scan.nextInt();
            if (eId == 0)
                this.viewCourse(courseID);
            else
                this.viewExercise(eId, courseID);
            return;
        case 2:
            this.addExercise(courseID);
            return;
        case 3:
            this.viewTA(courseID);
            this.viewCourse(courseID);
            return;
        case 4:
            if (this.role == 2) {
                this.prompt("You are not authorised to add TA");
                this.viewCourse(courseID);
                return;
            }
            this.addTA(courseID);
            this.viewCourse(courseID);
            return;
        case 5:
            if (this.role == 2)
                this.enrollByTA(courseID);
            else
                this.enroll(courseID);
            this.viewCourse(courseID);
            return;
        case 6:
            if (this.role == 2)
                this.dropByTA(courseID);
            else
                this.drop(courseID);
            this.viewCourse(courseID);
            return;
        case 7:
            this.viewQuestionBank(courseID);
            break;
        case 8:
            // show the list of all Question Bank ids and names
            this.prompt("Enter Question Bank ID");
            this.addQuestionBank(courseID);
            break;
        }
        // View Report
        return;
    }

    public void viewAllFixedQuestions() {
        try {
            String sql = "Select q.id, q.description, q.difficulty from fixed_question fq left join question q on fq.QID=q.ID";
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            this.prompt("ID\tDifficulty\tQuestion");
            while (rs.next()) {
                this.prompt(rs.getInt(1) + "\t" + rs.getInt(3) + "\t" + rs.getString(2));
            }
        } catch (SQLException e) {
            this.prompt("Failed to show all Fixed Questions" + e);
        }
    }

    public void createQuestionBank() {
        this.scan.nextLine();
        this.prompt("Enter name of the Question Bank");
        String qbname = this.scan.nextLine();

        String sql = "INSERT INTO question_bank(NAME) VALUES(?)";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, qbname);
            st.executeQuery();
            this.viewAllFixedQuestions();
            this.prompt("Enter each question id or -1 to stop adding");
            int k = scan.nextInt();
            while (k != -1) {
                sql = "INSERT INTO QUESTION_LIST VALUES(?, QUESTIONBANK_SEQ.currval)";
                st = connection.prepareStatement(sql);
                st.setInt(1, k);
                try {
                    st.executeQuery();
                    k = this.scan.nextInt();
                } catch (SQLException e) {
                    this.prompt("Invalid Question ID : " + e);
                    k = this.scan.nextInt();
                }
            }
        } catch (SQLException e) {
            this.prompt("Failed to create QB: " + e);
        }
    }

    public void viewQuestionBank(String courseID) {
        String sql = "Select qbid from course where id=?";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, courseID);
            ResultSet rs = st.executeQuery();
            int qbid = -1;
            if (rs.next()) {
                qbid = rs.getInt(1);
                if (qbid == 0) {
                    this.prompt("No question Bank added yet to this course");
                    this.viewCourse(courseID);
                    return;
                } else {
                    String sql2 = "Select id, name from question_bank where id=?";
                    String sql3 = "Select q.id, q.DESCRIPTION, q.DIFFICULTY from question_bank qb, course c, "
                            + "question_list ql, question q where c.qbid=qb.id and ql.QBID=qb.id "
                            + "and ql.QID=q.id and c.qbid = ?";
                    PreparedStatement st2 = connection.prepareStatement(sql2);
                    PreparedStatement st3 = connection.prepareStatement(sql3);
                    st2.setInt(1, qbid);
                    st3.setInt(1, qbid);
                    ResultSet rs2 = st2.executeQuery();
                    ResultSet rs3 = st3.executeQuery();
                    String name;
                    if (rs2.next()) {
                        name = rs2.getString(2);
                    }
                    this.prompt("ID\tDifficulty\tDescription");
                    while (rs3.next()) {
                        this.prompt(rs3.getInt(1) + "\t" + rs3.getInt(3) + "\t" + rs3.getString(2));
                    }
                }
            }
        } catch (SQLException e) {
            this.prompt("Failed to show course question bnk: " + e);
        }
    }

    public void addQuestionBank(String courseID) {
        String sql = "Select id, name from question_bank";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            this.prompt("ID\tName");
            while (rs.next()) {
                this.prompt(rs.getInt(1) + "\t" + rs.getString(2));
            }
            this.prompt("Enter ID of the question bank");
            int qbid = this.scan.nextInt();
            String sql2 = "Update course set qbid=? where id=?";
            PreparedStatement st2 = connection.prepareStatement(sql2);
            st2.setInt(1, qbid);
            st2.setString(2, courseID);
            st2.executeQuery();
            this.viewCourse(courseID);
            return;
        } catch (SQLException e) {
            this.prompt("Failed to add QB to course " + e);
        }
    }

    public void addCourse() throws SQLException {
        this.scan.nextLine();
        this.prompt("Enter CourseID");
        String courseId = this.scan.nextLine();
        this.prompt("Enter Course Name");
        String courseName = this.scan.nextLine();
        this.prompt("Enter Start Date : (mm/dd/yyyy)");
        String startDate = this.scan.nextLine();
        this.prompt("Enter End Date : (mm/dd/yyyy)");
        String endDate = this.scan.nextLine();
        this.prompt("Degree Level: Enter 0:Undergrad, 1:Grad");
        int degree = scan.nextInt();
        this.prompt("Enter Maximum Strength of students allowed ");
        int allow = this.scan.nextInt();
        // Insert the course
        String sql = "INSERT INTO COURSE(ID, NAME, PID, STARTDATE, ENDDATE, DEGREE, MAX_STUDENTS_ALLOWED) VALUES"
                + "(?, ?, ?, to_date(?, 'MM/DD/YYYY')" + ", to_date(?, 'MM/DD/YYYY'), ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseId);
        statement.setString(2, courseName);
        statement.setString(3, this.userId);
        statement.setString(4, startDate);
        statement.setString(5, endDate);
        statement.setInt(6, degree);
        statement.setInt(7, allow);
        // SimpleDateFormat format = new SimpleDateFormat("mm/dd/yyyy");
        /*
         * java.util.Date usd; java.util.Date ued; try { usd =
         * format.parse(startDate); ued = format.parse(endDate);
         * statement.setDate(4, new java.sql.Date(usd.getTime()));
         * statement.setDate(5, new java.sql.Date(ued.getTime())); } catch
         * (ParseException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */
        ResultSet result = statement.executeQuery();
        this.prompt("Enter 0 to return to main menu");
        int choice = this.scan.nextInt();
        if (choice == 0)
            this.instructorHandler();
        else {
            this.prompt("Wrong input. Exiting");
            return;
        }
    }

    public void showQuesInExe(int eid) throws SQLException {
        String sql = "Select q.id, q.description, q.difficulty from exercise_question eq, question q"
                + " where eq.qid=q.id and eid=?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, eid);
        ResultSet rs = statement.executeQuery();
        boolean isEmpty = true;
        while (rs.next()) {
            isEmpty = false;
            this.prompt("QueID : " + rs.getInt(1) + "\tQuestion: " + rs.getString(2) + "\tDifficulty: " + rs.getInt(3));
        }
        if (isEmpty)
            this.prompt("No Questions added to the exercise");
    }

    public void addQueToExercise(int eid, String courseID) throws SQLException {
        this.prompt("1: View Question bank to select 2: Select from Exercise Topic 0:Back");
        int choice = scan.nextInt();
        switch (choice) {
        case 0:
            this.viewExercise(eid, courseID);
            return;
        case 1:
            this.viewQuestionBank(courseID);
            this.prompt("Enter Question ID to add or -1 to return");
            int qid = this.scan.nextInt();
            while (qid != -1) {
                try {
                    String sql = "INSERT INTO EXERCISE_QUESTION VALUES(?,?)";
                    PreparedStatement st = connection.prepareStatement(sql);
                    st.setInt(1, qid);
                    st.setInt(2, eid);
                    st.executeQuery();
                } catch (SQLException e) {
                    this.prompt("Failed to add question to exercise : " + e);
                }
                qid = this.scan.nextInt();
            }
        case 2:
            String sql = "Select topic_id from exercise where id=?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, eid);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                this.utilityViewTopicQuestions(rs.getInt(1));
                this.prompt("Enter Question ID to add or -1 to return");
                qid = this.scan.nextInt();
                while (qid != -1) {
                    try {
                        String sql2 = "INSERT INTO EXERCISE_QUESTION VALUES(?,?)";
                        PreparedStatement st2 = connection.prepareStatement(sql2);
                        st2.setInt(1, qid);
                        st2.setInt(2, eid);
                        st2.executeQuery();
                    } catch (SQLException e) {
                        this.prompt("Failed to add question to exercise : " + e);
                    }
                    qid = this.scan.nextInt();
                }
            }
            return;
        }

    }

    public void viewExercise(int eid, String courseID) throws SQLException {
        this.prompt("0: Back to Menu 1: Show Questions 2: Add Questions 3: Remove Questions");
        int choice = this.scan.nextInt();
        switch (choice) {
        case 0:
            this.viewCourse(courseID);
            return;
        case 1:
            this.showQuesInExe(eid);
            this.prompt("0: Back to Previous Page 1: Back to previous Menu");
            choice = scan.nextInt();
            if (choice == 0)
                this.viewExercise(eid, courseID);
            else
                this.viewCourse(courseID);
            return;
        case 2:
            this.addQueToExercise(eid, courseID);
            return;
        case 3:
            // show the questions in the exercise and ask for the question id
            // Remove
            this.prompt("Enter Question ID");
            int qid = this.scan.nextInt();
            String sql = "DELETE FROM COURSE_EXERCISE_QUESTION where qid=? and eid=?";
            try {
                PreparedStatement st = connection.prepareStatement(sql);
                st.setInt(1, qid);
                st.setInt(2, eid);
                st.executeQuery();
                this.viewExercise(eid, courseID);
                return;
            } catch (SQLException e) {
                this.viewExercise(eid, courseID);
                return;
            }
        default:
            return;
        }
    }

    public void addExercise(String courseID) {
        this.prompt("Enter Mode (1: Standard 2:Adaptive)");
        int mode = scan.nextInt();
        this.scan.nextLine();
        this.prompt("Enter name of the exercise");
        String name = this.scan.nextLine();
        this.prompt("Enter Number of retries");
        int ret = scan.nextInt();
        this.prompt("Total number of questions");
        int num = scan.nextInt();
        this.prompt("Points for correct");
        int pc = scan.nextInt();
        this.prompt("Points for wrong");
        int nc = scan.nextInt();
        this.prompt("Scoring Policy");
        int sc = scan.nextInt();
        this.scan.nextLine();
        this.prompt("Enter Start Date : (mm/dd/yyyy)");
        String startDate = this.scan.nextLine();
        this.prompt("Enter Deadline : (mm/dd/yyyy)");
        String endDate = this.scan.nextLine();
        int minDiff, maxDiff;
        this.prompt("Enter Min Difficulty");
        minDiff = this.scan.nextInt();
        this.prompt("Enter Max Difficulty");
        maxDiff = this.scan.nextInt();
        this.viewTopics();
        this.prompt("Select Topic ID for this exercise");
        int topicid = this.scan.nextInt();
        // Add exercise to the existing course
        try {
            String sql = "INSERT INTO exercise(name, retries, pointsright, pointswrong, "
                    + "numquestions, startdate, enddate, emode, scoring_policy, "
                    + "min_difficulty, max_difficulty, topic_id) "
                    + "VALUES(?, ?, ?, ?, ?, to_date(?, 'MM/DD/YYYY'), to_date(?, 'MM/DD/YYYY')," + " ?, ?, ?, ?, ?)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, name);
            st.setInt(2, ret);
            st.setInt(3, pc);
            st.setInt(4, nc);
            st.setInt(5, num);
            st.setString(6, startDate);
            st.setString(7, endDate);
            st.setInt(8, mode);
            st.setInt(9, sc);
            st.setInt(10, minDiff);
            st.setInt(11, maxDiff);
            st.setInt(12, topicid);
            st.executeQuery();
            String sql2 = "INSERT INTO course_exercise values (?, EXERCISE_SEQ.currval)";
            PreparedStatement st2 = connection.prepareStatement(sql2);
            st2.setString(1, courseID);
            st2.executeQuery();
            this.prompt("Succesfully added exercise. View it back in menu to add Questions if Standard mode");
            this.viewCourse(courseID);
        } catch (SQLException e) {
            this.prompt("Failed to add Exercise to course" + e);
        }

    }

    public void viewTA(String courseID) throws SQLException {
        // Show all TA
        String sql = "Select S.userid, S.fname, S.lname from Enrollment E, Student S where E.sid=S.userid and E.cid=? and E.isTA=?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseID);
        statement.setInt(2, 1);
        ResultSet rs = statement.executeQuery();
        boolean isEmpty = true;
        this.prompt("---------------------Output------------------\n");
        while (rs.next()) {
            isEmpty = false;
            this.prompt("TA userid: " + rs.getString(1) + "\t Full Name: " + rs.getString(2) + " " + rs.getString(3));
        }
        if (isEmpty) {
            this.prompt("No TAs Assigned yet");
        }
        this.prompt("----------------------------------------------\n");
        return;
    }

    public void addTA(String courseID) throws SQLException {
        this.prompt("Enter Student UserID");
        this.scan.nextLine();
        String studentId = this.scan.nextLine();
        // check if he/she is enrolled in as a student
        // Add
        String sql = "Select isTA from enrollment where cid=? and sid=?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseID);
        statement.setString(2, studentId);
        ResultSet rs = statement.executeQuery();
        this.prompt("------------------Output------------------");
        if (rs.next()) {
            int isTa = rs.getInt(1);
            if (isTa == 1) {
                this.prompt(studentId + " is already added as TA before");
            } else {
                this.prompt(studentId + " is enrolled as Student. Cannot add him as TA");
            }
        } else {
            String sql3 = "Select * from student where userid=?";
            PreparedStatement statement3 = connection.prepareStatement(sql3);
            statement3.setString(1, studentId);
            ResultSet rs3 = statement3.executeQuery();
            if (!rs3.next()) {
                this.prompt("No student exists with the user id given. Try again");
                this.addTA(courseID);
                return;
            }
            sql = "INSERT into enrollment values(?, ?, ?)";
            PreparedStatement statement2 = connection.prepareStatement(sql);
            statement2.setString(1, studentId);
            statement2.setString(2, courseID);
            statement2.setInt(3, 1);
            try {
                statement2.executeQuery();
                this.prompt("Successfully added TA: " + studentId);
            } catch (SQLException se) {
                this.prompt("Failed to add TA: " + se.toString());
            }
        }
        this.prompt("-------------------------------------------------");
        return;
    }

    /*
     * public int enrollOrDropStudent(String courseID) {
     * this.prompt("1: Enroll 2: Drop"); int choice = this.scan.nextInt();
     * this.prompt("Enter Student ID"); String studentID = this.scan.next();
     * this.prompt("Enter First Name"); String firstName = this.scan.nextLine();
     * this.prompt("Enter Last Name"); String lastName = this.scan.nextLine();
     * if (choice == 1) { // enroll } else if (choice == 2) { // drop } return
     * 0; }
     */

    public void searchOrAddQue() throws SQLException {
        // view all Question bank ids and names
        this.prompt("Enter Question Bank ID to view Or 0: Add new Question Bank");
        int choice = scan.nextInt();
        if (choice == 0) {
            this.prompt("Enter Name of the new question bank");
            // add it
        } else {
            // show the questions in the given question bank id
            this.prompt("Enter question ID to view or 2: Add question to Bank  0: Go back to previous menu");
            choice = scan.nextInt();
            if (choice == 0) {
                this.instructorHandler();
            } else if (choice == 2) {
                this.addQuestion();
            } else {
                // view the question with question ID choice
            }
        }
        return;
    }

    public void studentHandler() throws SQLException {
        int choice = this.viewStudentMenu();
        switch (choice) {
        case 1:
            this.viewStudentProfile();
        case 2:
            this.viewCurrentCourses();
        case 3:
            return;
        default:
            this.prompt("Invalid Input");
            this.studentHandler();
            break;
        }
        return;
    }

    public int viewStudentMenu() {
        this.prompt("1.View/Edit Profile  2.View courses 3.Logout");
        while (true) {
            try {
                int choice = scan.nextInt();
                if (choice >= 1 && choice <= 3)
                    return choice;
            } catch (Exception e) {
                this.prompt("Invalid Input");
            }
        }
    }

    public void viewStudentProfile() throws SQLException {
        String firstname = null;
        String lastname = null;
        String studentID = null;
        String sql = "SELECT * FROM student WHERE userid = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, this.userId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            firstname = resultSet.getString("fname");
            lastname = resultSet.getString("lname");
            studentID = this.userId;
            break;
        }
        this.prompt("First Name: " + firstname);
        this.prompt("Last Name: " + lastname);
        this.prompt("Student ID: " + studentID);
        this.prompt("Enter 1 to edit profile");
        this.prompt("Press 0 to get back to Menu");
        int choice = this.scan.nextInt();
        while (true) {
            if (choice == 0)
                this.studentHandler();
            else if (choice == 1) {
                this.editStudentProfile();
            } else {
                this.prompt("Wrong input. Please enter again.");
            }
        }
    }

    public void editStudentProfile() throws SQLException {
        String firstname = null;
        String lastname = null;
        this.prompt("Enter firstname and lastname");
        firstname = this.scan.next();
        lastname = this.scan.next();
        String sql = "UPDATE student SET fname = ?, lname = ? WHERE userid = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, firstname);
        statement.setString(2, lastname);
        statement.setString(3, userId);
        statement.executeQuery();
        this.prompt("Profile updated successfully");
        this.studentHandler();
    }

    public void viewCurrentCourses() throws SQLException {
        String sql = "SELECT c.id, c.name FROM course c JOIN enrollment e ON e.cid = c.id WHERE e.sid = ? AND e.ista = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setInt(2, 0);
        ResultSet resultSet = statement.executeQuery();
        this.userCourses = new ArrayList<String>();
        while (resultSet.next()) {
            this.prompt(resultSet.getString("id") + ": " + resultSet.getString("name"));
            this.userCourses.add(resultSet.getString("id"));
        }
        this.prompt("Enter 1 to view course");
        this.prompt("Enter 0 to return to previous menu");
        int option = scan.nextInt();
        switch (option) {
        case 0:
            this.studentHandler();
            return;
        case 1:
            this.viewCourse();
            break;
        default:
            this.prompt("Invalid Input");
            this.studentHandler();
        }
    }

    public void viewCourse() throws SQLException {
        this.prompt("Enter course id");
        String courseId = this.scan.next();
        if (!this.userCourses.contains(courseId)) {
            this.prompt("Course not found in enrolled courses.");
            this.viewCourse();
        }
        this.prompt("Enter 1 to view current homeworks");
        this.prompt("Enter 2 to view past homeworks");
        this.prompt("Enter 0 to return to start screen");
        int option = scan.nextInt();
        switch (option) {
        case 0:
            this.studentHandler();
        case 1:
            this.viewCurrentHWs(courseId);
        case 2:
            this.viewPastHWs(courseId);
        default:
            this.prompt("Invalid Input");
            this.studentHandler();
        }
    }

    public void viewPastHWs(String courseId) throws SQLException {
        String sql = "SELECT e.id, e.name " + "FROM course c JOIN course_exercise ce ON ce.cid = c.id "
                + "JOIN exercise e ON e.id = ce.eid WHERE ((SELECT current_timestamp FROM DUAL) "
                + "> e.ENDDATE) AND c.id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseId);
        ResultSet resultSet = statement.executeQuery();
        this.prompt("Past Homeworks: ");
        this.userHws = new ArrayList<Integer>();
        while (resultSet.next()) {
            this.prompt(resultSet.getString("id") + ": " + resultSet.getString("name"));
            this.userHws.add(resultSet.getInt("id"));
        }
        if (userHws.size() == 0)
            this.viewCourse();
        this.prompt("Enter 1 to view report");
        this.prompt("Enter 0 to return to previous menu");
        int option = scan.nextInt();
        switch (option) {
        case 0:
            this.studentHandler();
        case 1:
            this.viewHWReport(courseId);
        default:
            this.prompt("Invalid Input");
            this.studentHandler();
        }
    }

    public void viewHWReport(String courseId) throws SQLException {
        this.prompt("Enter HW id");
        int hwId = scan.nextInt();
        if (!this.userHws.contains(hwId)) {
            this.prompt("HW not found in submitted HW's.");
            this.viewHWReport(courseId);
        }
        String sql = "SELECT sub.score, sub.details FROM submission sub WHERE sub.eid = ? and sub.sid = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, hwId);
        statement.setString(2, this.userId);
        ResultSet resultSet = statement.executeQuery();
        List<Integer> scores = new ArrayList<Integer>();
        while (resultSet.next()) {
            this.prompt("Score: " + resultSet.getInt("score") + " Details: " + resultSet.getString("details"));
            scores.add(resultSet.getInt("score"));
        }
        String policy = this.calculateFinalScore(hwId);
        float score = 0;
        if (policy.equals("Latest Attempt")) {
            score = scores.get(scores.size() - 1);
        }
        else if (policy.equals("Maximum Score")) {
            int maxScore = Integer.MIN_VALUE;
            for (Integer sc : scores) {
                maxScore = sc > maxScore ? sc : maxScore;
            }
            score = maxScore;
        }
        else if (policy.equals("Average Score")) {
            int avg = 0;
            int total = 0;
            for (Integer sc : scores) {
                avg += sc;
                total++;
            }
            score = avg/total;
        }
        this.prompt("Final Score: " + score);
        this.prompt("Enter 1 to view report");
        this.prompt("Enter 0 to return to previous menu");
        int option = scan.nextInt();
        switch (option) {
        case 0:
            this.viewPastHWs(courseId);
        case 1:
            this.viewHWReport(courseId);
        default:
            this.prompt("Invalid Input");
            this.viewHWReport(courseId);
        }
    }

    public String calculateFinalScore(int hwId) throws SQLException {
        String policy = "";
        String sql = "SELECT scoring_policy FROM exercise WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, hwId);
        ResultSet resultSet = statement.executeQuery();
        int scoringPolicy = 0;
        while (resultSet.next()) {
            scoringPolicy = resultSet.getInt(1);
        }
        sql = "SELECT name FROM scoring_policy where id = ?";
        statement = connection.prepareStatement(sql);
        statement.setInt(1, scoringPolicy);
        resultSet = statement.executeQuery();
        while (resultSet.next()) {
            policy = resultSet.getString(1);
        }
        return policy;
    }

    public void viewCurrentHWs(String courseId) throws SQLException {
        String sql = "SELECT e.id, e.name " + "FROM course c JOIN course_exercise ce ON ce.cid = c.id "
                + "JOIN exercise e ON e.id = ce.eid WHERE (SELECT current_timestamp FROM DUAL) "
                + "BETWEEN e.startdate and e.ENDDATE AND c.id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, courseId);
        ResultSet resultSet = statement.executeQuery();
        this.prompt("Current Homeworks: ");
        this.userHws = new ArrayList<Integer>();
        while (resultSet.next()) {
            this.prompt(resultSet.getInt("id") + ": " + resultSet.getString("name"));
            this.userHws.add(resultSet.getInt("id"));
        }
        if (userHws.size() == 0)
            this.viewCourse();
        this.prompt("Enter 1 to attempt HW");
        this.prompt("Enter 0 to return to previous menu");
        int option = scan.nextInt();
        switch (option) {
        case 0:
            this.studentHandler();
        case 1:
            this.attemptHW(courseId);
        default:
            this.prompt("Invalid Input");
            this.studentHandler();
        }
    }

    public boolean checkIfEligible(String courseId, int exerciseId) {
        String sql1 = "Select retries from exercise where id=?";
        String sql2 = "Select count(*) from submission where sid=? and eid=?";
        try {
            PreparedStatement st = connection.prepareStatement(sql1);
            PreparedStatement st2 = connection.prepareStatement(sql2);
            st2.setString(1, this.userId);
            st2.setInt(2, exerciseId);
            st.setInt(1, exerciseId);
            ResultSet rs1 = st.executeQuery();
            ResultSet rs2 = st2.executeQuery();
            int a = 0, b = 0;
            if (rs1.next())
                a = rs1.getInt(1);
            if (rs2.next())
                b = rs2.getInt(1);
            if (a <= b)
                return false;
        } catch (SQLException e) {
            this.prompt("Failed to check retries" + e);
            return false;
        }
        return true;
    }

    public void attemptHW(String courseId) throws SQLException {
        this.prompt("Enter HW id");
        int hwId = scan.nextInt();
        if (!this.userHws.contains(hwId)) {
            this.prompt("HW not found in current HW's.");
            this.viewCurrentHWs(courseId);
        }
        if (!this.checkIfEligible(courseId, hwId)) {
            this.prompt("You exceed the number of tries");
            return;
        }
        String sql = "SELECT q.id, q.description FROM exercise_question eq JOIN question q ON q.id = eq.qid where eq.eid = ?";
        String answerSql = "SELECT a.id, a.answer, a.iscorrect FROM answer a where a.qid = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, hwId);
        ResultSet resultSet = statement.executeQuery();
        int marks = 0;
        int right = 1, wrong = 0;
        String sql2 = "Select pointsright, pointswrong from exercise where id=?";
        PreparedStatement st2 = connection.prepareStatement(sql2);
        st2.setInt(1, hwId);
        ResultSet rs2 = st2.executeQuery();
        if (rs2.next()) {
            right = rs2.getInt(1);
            wrong = rs2.getInt(2);
        }
        List<Integer> correctAns = new ArrayList<Integer>();
        List<Integer> correctRes = new ArrayList<Integer>();
        List<Integer> wrongRes = new ArrayList<Integer>();
        while (resultSet.next()) {
            this.prompt(resultSet.getInt(1) + ": " + resultSet.getString(2));
            PreparedStatement ansStatement = connection.prepareStatement(answerSql);
            ansStatement.setString(1, resultSet.getString("id"));
            ResultSet ansSet = ansStatement.executeQuery();

            while (ansSet.next()) {
                this.prompt(ansSet.getInt(1) + "\t" + ansSet.getString("answer"));
                if (ansSet.getInt(3) == 1)
                    correctAns.add(ansSet.getInt(1));
            }
            this.prompt("Select an id of your answer");
            int selection = this.scan.nextInt();
            if (correctAns.contains(selection)) {
                marks += right;
                correctRes.add(resultSet.getInt(1));
            } else {
                wrongRes.add(resultSet.getInt(1));
                marks -= wrong;
            }
        }
        this.scan.nextLine();
        sql = "INSERT INTO SUBMISSION(score, details, sid, eid) " + "VALUES(?, 'feedback', ?, ?)";
        st2 = connection.prepareStatement(sql);
        st2.setInt(1, marks);
        st2.setString(2, this.userId);
        st2.setInt(3, hwId);
        st2.executeQuery();
        this.prompt("Your Score :" + marks);
        this.prompt("Correctly answered Questions");
        for (Integer t : correctRes) {
            System.out.print(" " + t);
        }
        this.prompt("");
        this.prompt("Wrongly answered Questions");
        for (Integer t : wrongRes) {
            System.out.print(t + " ");
        }
        return;
    }

    public void getConnection() throws SQLException {
        if (this.connection != null)
            return;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your Oracle JDBC Driver?");
            e.printStackTrace();
            return;
        }
        this.connection = DriverManager.getConnection(jdbcURL, "rpotlur", "200132312");
    }

    public void tahandler() {
        this.prompt("Course ID\t CourseName");
        String sql = "Select c.id, c.name from course c, enrollment e " + "where c.id=e.cid and e.sid=? and e.ista=?";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, this.userId);
            st.setInt(2, 1);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                this.prompt("ID: " + rs.getString(1) + " Name: " + rs.getString(2));
            }
            this.scan.nextLine();
            this.prompt("Enter courseID");
            String courseID = this.scan.nextLine();
            this.viewCourse(courseID);
        } catch (SQLException e) {
            this.prompt("FAiled to show TA courses" + e);
        }
        this.prompt("Course ID\t CourseName");
        sql = "Select c.cid, c.name from course c, enrollment e " + "where e.sid=? and e.ista=1";
        try {
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, this.userId);
            ResultSet rs = st.executeQuery();
            this.prompt("Select course ID");
            while (rs.next()) {
                this.prompt("ID: " + rs.getString(1) + " Name: " + rs.getString(2));
            }
            this.scan.nextLine();
            String select = this.scan.nextLine();
            this.prompt("1: Enroll a student 2: Drop a student 3: View/Create exercise");
        } catch (SQLException e) {
            this.prompt("FAiled to show TA courses" + e);
        }
    }

    public void start() throws SQLException {
        String username = "";
        String password = "";
        // Get primary key of student/instructor/ta
        this.userId = "";
        this.prompt("Enter Username and Password");
        try {
            username = scan.next();
            password = scan.next();
        } catch (Exception e) {
            this.prompt("Invalid input ");
            this.start();
        }
        this.role = this.getRole(username, password);
        switch (this.role) {
        case -1:
            this.prompt("Login Incorrect. Returning to Start menu .... ");
            Application.main(null);
            break;
        case 0:
            this.prompt("Welcome Instructor");
            this.instructorHandler();
            this.prompt("Logged Out");
            this.start();
            break;
        case 1:
            this.prompt("Welcome Student : " + username);
            this.studentHandler();
            this.prompt("Logged Out");
            this.start();
            break;
        case 2:
            this.prompt("Welcome TA : " + username);
            this.prompt("1: View My Student Profile 2: TA Actions 0: Logout");
            int opt = this.scan.nextInt();
            switch (opt) {
            case 0:
                return;
            case 1:
                this.role = 1;
                this.studentHandler();
                return;
            case 2:
                this.role = 2;
                this.tahandler();
                return;
            default:
                break;
            }
            break;
        default:
            this.prompt("Something went wrong");
            this.start();
            break;
        }
        return;
    }

    public int getRole(String username, String password) throws SQLException {
        /*
         * Search professor table. return 0 if found Search Student table.
         * return 1 Search if enrolled as TA. return 2 else return -1
         */
        String professor_sql = "SELECT * FROM professor WHERE userid = ? AND password = ?";
        Boolean isProfessor = this.isUser(professor_sql, username, password);
        if (isProfessor)
            return 0;
        String student_sql = "SELECT * FROM student WHERE userid = ? AND password = ?";
        Boolean isStudent = this.isUser(student_sql, username, password);
        if (isStudent && this.isTA(username))
            return 2;
        else if (isStudent)
            return 1;
        return -1;
    }

    public Boolean isTA(String username) throws SQLException {
        String taSql = "SELECT * FROM enrollment WHERE sid = ?";
        PreparedStatement statement = connection.prepareStatement(taSql);
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            if (resultSet.getBoolean(3))
                return true;
        }
        return false;
    }

    public Boolean isUser(String sql, String username, String password) throws SQLException {
        this.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.setString(2, password);
        ResultSet resultSet = statement.executeQuery();
        boolean empty = true;
        while (resultSet.next()) {
            empty = false;
            this.userId = resultSet.getString("userid");
            break;
        }
        if (!empty)
            return true;
        return false;
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Application app = new Application(scan);
        System.out.println("1.Login  2.Exit");
        int choice = scan.nextInt();
        switch (choice) {
        case 1:
            try {
                app.start();
            } catch (SQLException e) {
                app.prompt(e.toString());
            }
            break;
        case 2:
            return;
        default:
            app.prompt("Invalid Input");
            return;
        }
        try {
            app.connection.close();
        } catch (SQLException e) {
            app.prompt(e.toString());
        }
        scan.close();
    }
}
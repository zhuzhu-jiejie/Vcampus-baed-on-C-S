package seu.vcampus.model;

public class TeacherInfo {
    private String userid;      // 教师工号
    private String name;        // 姓名
    private String gender;      // 性别
    private String birth;       // 出生日期
    private String idCard;      // 身份证号
    private String phone;       // 手机号
    private String email;       // 邮箱
    private String politics;    // 政治面貌
    private String college;     // 所属学院
    private String department;  // 所属系部
    private String title;       // 职称
    private String entryYear;   // 入职年份
    private String major;       // 主讲专业
    private String education;   // 学历
    private String degree;      // 学位

    // Getter和Setter方法
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBirth() { return birth; }
    public void setBirth(String birth) { this.birth = birth; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPolitics() { return politics; }
    public void setPolitics(String politics) { this.politics = politics; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEntryYear() { return entryYear; }
    public void setEntryYear(String entryYear) { this.entryYear = entryYear; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
}
package cn.habitdiary.form.entity;


import java.io.Serializable;

/**
 * 反馈实体类
 */
public class Feedback implements Serializable {
    private Integer feedbackid; //反馈编号
    private Form form; //所属表单
    private String feedbacktime; //反馈时间
    private Integer rownumber; //所在行数
    private Integer feedbackstatus; //反馈状态:0表示待处理,1表示已处理

    public Feedback() {
    }

    public Integer getFeedbackid() {
        return feedbackid;
    }

    public void setFeedbackid(Integer feedbackid) {
        this.feedbackid = feedbackid;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public String getFeedbacktime() {
        return feedbacktime;
    }

    public void setFeedbacktime(String feedbacktime) {
        this.feedbacktime = feedbacktime;
    }

    public Integer getFeedbackstatus() {
        return feedbackstatus;
    }

    public void setFeedbackstatus(Integer feedbackstatus) {
        this.feedbackstatus = feedbackstatus;
    }

    public Integer getRownumber() {
        return rownumber;
    }

    public void setRownumber(Integer rownumber) {
        this.rownumber = rownumber;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "feedbackid=" + feedbackid +
                ", form=" + form +
                ", feedbacktime='" + feedbacktime + '\'' +
                ", rownumber=" + rownumber +
                ", feedbackstatus=" + feedbackstatus +
                '}';
    }
}

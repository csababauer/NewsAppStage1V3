package com.example.csaba.newsappstage1v3;

public class Event {

    public final String title;
    public final String url;
    public final String section;
    public final String date;


    public Event(String eventTitle, String eventUrl, String eventSection, String eventDate) {
        title = eventTitle;
        url = eventUrl;
        section = eventSection;
        date = eventDate;
    }

    public String getTitle() {
        return title;
    }
    public String getUrl() {
        return url;
    }
    public String getSection () {return section; }
    public String getDate() {return date;}
}

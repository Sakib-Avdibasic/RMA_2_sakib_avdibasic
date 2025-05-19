package com.example.rma_2_sakib_avdibasic.models;
import java.util.Date;

public class Movie {
    public String id;
    public String name;
    public String director;
    public String genre;
    public int year;
    public Date releaseDate;
    public int runtime;
    public String posterUrl;
    public String trailerUrl;
    public String settingPlace;
    public int settingYear;
    public boolean hasReminder;

    public Movie(String name, String director,
                 String genre, int year, int runtime, String settingPlace, int settingYear, String posterUrl, String trailerUrl) {
        this.name = name;
        this.director = director;
        this.genre = genre;
        this.year = year;
        this.runtime = runtime;
        this.settingYear = settingYear;
        this.settingPlace = settingPlace;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
        this.releaseDate = null;
    }
}

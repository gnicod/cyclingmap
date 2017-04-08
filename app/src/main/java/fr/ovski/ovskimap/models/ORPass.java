package fr.ovski.ovskimap.models;

public class ORPass {
    private int id;
    private String name;
    private Double lat;
    private Double lng;
    private int alt;
    private String desc;

    public ORPass(int id, String name, Double lat, Double lng, int alt, String desc) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public int getAlt() {
        return alt;
    }

    public void setAlt(int alt) {
        this.alt = alt;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name;
    }
}

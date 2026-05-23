package com.pixserve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimResponse {

    @JsonProperty("place_id")
    private Long placeId;
    
    private String licence;
    
    @JsonProperty("osm_type")
    private String osmType;
    
    @JsonProperty("osm_id")
    private Long osmId;
    
    private String lat;
    private String lon;
    
    @JsonProperty("class")
    private String className; // 'class' is a reserved keyword in Java
    
    private String type;
    
    @JsonProperty("place_rank")
    private Integer placeRank;
    
    private Double importance;
    private String addresstype;
    private String name;
    
    @JsonProperty("display_name")
    private String displayName;
    
    private Address address;
    private List<String> boundingbox;

    // Getters and Setters
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }

    public String getLicence() { return licence; }
    public void setLicence(String licence) { this.licence = licence; }

    public String getOsmType() { return osmType; }
    public void setOsmType(String osmType) { this.osmType = osmType; }

    public Long getOsmId() { return osmId; }
    public void setOsmId(Long osmId) { this.osmId = osmId; }

    public String getLat() { return lat; }
    public void setLat(String lat) { this.lat = lat; }

    public String getLon() { return lon; }
    public void setLon(String lon) { this.lon = lon; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getPlaceRank() { return placeRank; }
    public void setPlaceRank(Integer placeRank) { this.placeRank = placeRank; }

    public Double getImportance() { return importance; }
    public void setImportance(Double importance) { this.importance = importance; }

    public String getAddresstype() { return addresstype; }
    public void setAddresstype(String addresstype) { this.addresstype = addresstype; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public List<String> getBoundingbox() { return boundingbox; }
    public void setBoundingbox(List<String> boundingbox) { this.boundingbox = boundingbox; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String road;
        private String town;
        private String county;
        
        @JsonProperty("state_district")
        private String stateDistrict;
        
        private String state;
        
        @JsonProperty("ISO3166-2-lvl4")
        private String isoCode;
        
        private String postcode;
        private String country;
        
        @JsonProperty("country_code")
        private String countryCode;

        // Getters and Setters
        public String getRoad() { return road; }
        public void setRoad(String road) { this.road = road; }

        public String getTown() { return town; }
        public void setTown(String town) { this.town = town; }

        public String getCounty() { return county; }
        public void setCounty(String county) { this.county = county; }

        public String getStateDistrict() { return stateDistrict; }
        public void setStateDistrict(String stateDistrict) { this.stateDistrict = stateDistrict; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getIsoCode() { return isoCode; }
        public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

        public String getPostcode() { return postcode; }
        public void setPostcode(String postcode) { this.postcode = postcode; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }
}

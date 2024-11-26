package com.example.hairwise;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CompoundResponse {
    @SerializedName("InformationList")
    private InformationList informationList;

    public InformationList getInformationList() {
        return informationList;
    }

    public void setInformationList(InformationList informationList) {
        this.informationList = informationList;
    }

    public static class InformationList {
        @SerializedName("Information")
        private List<Information> information;

        public List<Information> getInformation() {
            return information;
        }

        public void setInformation(List<Information> information) {
            this.information = information;
        }
    }

    public static class Information {
        @SerializedName("CID")
        private int cid;

        @SerializedName("Title")
        private String title;

        @SerializedName("Description")
        private String description;

        @SerializedName("DescriptionSourceName")
        private String descriptionSourceName;

        @SerializedName("DescriptionURL")
        private String descriptionURL;

        // Getters e Setters
        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescriptionSourceName() {
            return descriptionSourceName;
        }

        public void setDescriptionSourceName(String descriptionSourceName) {
            this.descriptionSourceName = descriptionSourceName;
        }

        public String getDescriptionURL() {
            return descriptionURL;
        }

        public void setDescriptionURL(String descriptionURL) {
            this.descriptionURL = descriptionURL;
        }

        @Override
        public String toString() {
            return "Information{" +
                    "cid=" + cid +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", descriptionSourceName='" + descriptionSourceName + '\'' +
                    ", descriptionURL='" + descriptionURL + '\'' +
                    '}';
        }
    }
}


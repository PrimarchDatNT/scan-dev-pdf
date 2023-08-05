package com.document.camerascanner.features.detailshow.detailpage;

class PagePreview {

    private int roation;

    private String imgUri;

    public PagePreview(int roation, String imgUri) {
        this.roation = roation;
        this.imgUri = imgUri;
    }

    public void setRoation() {
        this.roation += 90;

        if (this.roation > 360) {
            this.roation = this.roation - 360;
        }
    }

    public int getRoation() {
        return this.roation;
    }

    public String getImgUri() {
        return this.imgUri;
    }

    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }
}

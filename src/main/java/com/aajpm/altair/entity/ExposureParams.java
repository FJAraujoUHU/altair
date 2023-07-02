package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.Collection;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "exposure_params")
public class ExposureParams extends BasicEntity implements Serializable {
    
    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @NotNull
    @Column(name = "is_light_frame", nullable = false)
    private Boolean lightFrame;

    @Positive
    @NotNull
    @Column(name = "exposure_time", nullable = false)
    private Double exposureTime;

    @NotNull
    @Column(name = "filter", nullable = false)
    private String filter;

    @NotNull
    @Min(1)
    @Column(name = "binX", nullable = false)
    private Integer binX;

    @NotNull
    @Min(1)
    @Column(name = "binY", nullable = false)
    private Integer binY;

    /** Placeholder for possible future upgrade, it does nothing for now */
    @Column(name = "gain", nullable = true)
    private String gain;

    @PositiveOrZero
    @Column(name = "subFrameX", nullable = true)
    private Integer subFrameX;

    @PositiveOrZero
    @Column(name = "subFrameY", nullable = true)
    private Integer subFrameY;

    @Min(1)
    @Column(name = "subFrameWidth", nullable = true)
    private Integer subFrameWidth;

    @Min(1)
    @Column(name = "subFrameHeight", nullable = true)
    private Integer subFrameHeight;



    //#region Getters & Setters

        /**
         * Returns whether the image is a light frame.
         * 
         * @return true if the image is a light frame, false otherwise.
         */
        public Boolean isLightFrame() {
            return lightFrame;
        }

        /**
         * Sets whether the image is a light frame.
         * 
         * @param useLightFrame true if the image is a light frame, false otherwise.
         */
        public void setLightFrame(Boolean useLightFrame) {
            this.lightFrame = useLightFrame;
        }

        /**
         * Returns the exposure time in seconds.
         * 
         * @return the exposure time in seconds.
         */
        public Double getExposureTime() {
            return exposureTime;
        }

        /**
         * Sets the exposure time in seconds.
         * 
         * @param exposureTimeSeconds the exposure time in seconds.
         */
        public void setExposureTime(Double exposureTimeSeconds) {
            this.exposureTime = exposureTimeSeconds;
        }

        /**
         * Returns the filter used for the image.
         * 
         * @return the name of the filter used for the image.
         */
        public String getFilter(){
            return filter;
        }

        /**
         * Sets the filter used for the image.
         * 
         * @param filter the name of the filter used for the image.
         */
        public void setFilter(String filter){
            this.filter = filter;
        }

        /**
         * Returns the binning factor in the X direction.
         * 
         * @return the binning factor in the X direction.
         */
        public Integer getBinX() {
            return binX;
        }

        /**
         * Sets the binning factor in the X direction.
         * 
         * @param binX the binning factor in the X direction.
         */
        public void setBinX(Integer binX) {
            this.binX = binX;
        }

        /**
         * Returns the binning factor in the Y direction.
         * 
         * @return the binning factor in the Y direction.
         */
        public Integer getBinY() {
            return binY;
        }

        /**
         * Returns the gain.
         * 
         * @return the gain.
         * @apiNote Placeholder for possible future upgrade, it does nothing for now
         */
        public String getGain() {
            return gain;
        }

        /**
         * Sets the gain.
         * 
         * @param gain the gain.
         * @apiNote Placeholder for possible future upgrade, it does nothing for now
         */
        public void setGain(String gain) {
            this.gain = gain;
        }

        /**
         * Sets the binning factor in the Y direction.
         * 
         * @param binY the binning factor in the Y direction.
         */
        public void setBinY(Integer binY) {
            this.binY = binY;
        }

        /**
         * Returns the X coordinate of the subframe.
         * 
         * @return the X coordinate of the subframe, in pixels starting from the left of the image.
         *         If the subframe is not used, returns {@code null}.
         */
        public Integer getSubFrameX() {
            return subFrameX;
        }

        /**
         * Sets the X coordinate of the subframe.
         * 
         * @param subFrameX the X coordinate of the subframe, in pixels starting from the left of the image.
         *                  Set as {@code null} if the subframe is not used.
         */
        public void setSubFrameX(Integer subFrameX) {
            this.subFrameX = subFrameX;
        }

        /**
         * Returns the Y coordinate of the subframe.
         * 
         * @return the Y coordinate of the subframe, in pixels starting from the top of the image.
         *         If the subframe is not used, returns {@code null}.
         */
        public Integer getSubFrameY() {
            return subFrameY;
        }

        /**
         * Sets the Y coordinate of the subframe.
         * 
         * @param subFrameY the Y coordinate of the subframe, in pixels starting
         *                  from the top of the image. Set as {@code null} if
         *                  the subframe is not used.
         */
        public void setSubFrameY(Integer subFrameY) {
            this.subFrameY = subFrameY;
        }

        /**
         * Returns the width of the subframe.
         * 
         * @return the width of the subframe, in pixels.
         */
        public Integer getSubFrameWidth() {
            return subFrameWidth;
        }

        /**
         * Sets the width of the subframe.
         * 
         * @param subFrameWidth the width of the subframe, in pixels.
         */
        public void setSubFrameWidth(Integer subFrameWidth) {
            this.subFrameWidth = subFrameWidth;
        }

        /**
         * Returns the height of the subframe.
         * 
         * @return the height of the subframe, in pixels.
         */
        public Integer getSubFrameHeight() {
            return subFrameHeight;
        }

        /**
         * Sets the height of the subframe.
         * 
         * @param subFrameHeight the height of the subframe, in pixels.
         */
        public void setSubFrameHeight(Integer subFrameHeight) {
            this.subFrameHeight = subFrameHeight;
        }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Program program;

    @OneToMany(mappedBy = "exposureParams", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<ExposureOrder> exposureOrders;


    //#region Getters & Setters
    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Collection<ExposureOrder> getExposureOrders() {
        return exposureOrders;
    }

    public void setExposureOrders(Collection<ExposureOrder> exposureOrders) {
        this.exposureOrders = exposureOrders;
    }

    public void addExposureOrder(ExposureOrder exposureOrder) {
        this.exposureOrders.add(exposureOrder);
    }

    public void removeExposureOrder(ExposureOrder exposureOrder) {
        this.exposureOrders.remove(exposureOrder);
    }
    //#endregion

    //#endregion
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods

    public boolean usesSubFrame() {
        return subFrameX != null || subFrameY != null || subFrameWidth != null || subFrameHeight != null;
    }

    //#endregion
}

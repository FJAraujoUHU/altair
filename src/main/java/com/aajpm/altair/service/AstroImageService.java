package com.aajpm.altair.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ImageAttribute;
import com.aajpm.altair.entity.ImageValue;
import com.aajpm.altair.repository.AstroImageRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.Interval;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;

import com.aajpm.altair.entity.AstroImage;
import com.aajpm.altair.entity.AstroObject;

@Service
@Transactional
public class AstroImageService extends BasicEntityCRUDService<AstroImage> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private AstroImageRepository astroImageRepository;

    @Override
    protected AstroImageRepository getManagedRepository() {
        return astroImageRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    @Autowired
    private ImageAttributeService imageAttributeService;

    @Autowired
    private ImageValueService imageValueService;

    @Autowired
    private AstroObjectService astroObjectService;

    ////////////////////////////// CONFIGURATION //////////////////////////////

    @Autowired
    private ObservatoryService observatoryService;
    
    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public AstroImageService() {
        super();
    }

    @Override
    public AstroImage create() {
        return new AstroImage();
    }

    /**
     * Creates an {@link AstroImage} with the given file name and creation date.
     * 
     * @param fileName The file name of the image.
     * @param creationDate The creation date of the image.
     * 
     * @return A new {@link AstroImage}.
     */
    public AstroImage create(String filename, Instant creationDate) {
        AstroImage image = create();
        image.setFileName(filename);
        image.setCreationDate(creationDate);
        return image;
    }

    /**
     * Creates an {@link AstroImage} from the existing FITS file at the given path.
     * 
     * @param path The path to the FITS file.
     * 
     * @return A new {@link AstroImage}, populated with the FITS header values.
     * @throws IOException
     */
    public AstroImage create(Path path) throws FitsException, IOException {
        Assert.notNull(path, "The path cannot be null.");
        Assert.isTrue(Files.exists(path), "The path must exist.");
        Assert.isTrue(Files.isRegularFile(path), "The path must be a regular file.");

        try (Fits fits = new Fits(path.toFile())) {
            return create(path.getFileName().toString(), fits);
        }
    }

    /**
     * Creates an {@link AstroImage} from the given FITS file.
     * 
     * @param fits The FITS file to create the image from.
     * @param filename The name of the FITS file.
     * 
     * @return A new {@link AstroImage}, populated with the FITS header values.
     * 
     * @throws FitsException If the FITS file header could not be read.
     * @throws IOException If the underlying buffer of the FITS file threw an error.
     * 
     * @implSpec Equivalent to: <pre>{@code create((ImageHDU) fits.readHDU());}</pre>
     */
    public AstroImage create(String filename, Fits fits) throws FitsException, IOException {
        return create(filename, (ImageHDU) fits.readHDU());
    }

    /**
     * Creates an {@link AstroImage} from the given FITS image header data unit.
     * 
     * @param hdu The FITS {@link ImageHDU} to create the image from.
     * @return A new {@link AstroImage}, populated with the FITS header values.
     */
    public AstroImage create(String filename, ImageHDU hdu) {
        AstroImage image = create();
        image.setFileName(filename);
        image.setCreationDate(hdu.getCreationDate().toInstant());

        // Set the target object, if it is present in the HDU and the DB
        AstroObject target = astroObjectService.findByName(hdu.getObject());
        if (target != null) {
            image.setTarget(target);
        }

        Collection<ImageAttribute> supportedAttributes = imageAttributeService.findAll();
        for (ImageAttribute attribute : supportedAttributes) {
            String value = hdu.getTrimmedString(attribute.getFitsKeyword());
            if (value != null) {
                ImageValue imgVal = imageValueService.create();
                imgVal.setImage(image);
                imgVal.setAttribute(attribute);
                imgVal.setValue(value);
                image.addValue(imgVal);
            }
        }

        return image;
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////
 
    @Override
    public AstroImage save(AstroImage image) {
        Assert.notNull(image, "The image cannot be null.");

        // Filename validation
        Assert.hasText(image.getFileName(), "The image must have a file name.");
        String validFileNameRegex = "[^a-zA-Z0-9\\._\\-]";
        Assert.isTrue(!image.getFileName().matches(validFileNameRegex), "The image file name contains illegal characters.");

        Path imgStorePath = observatoryService.getConfig().getCamera().getImageStorePath();
        Path filePath = imgStorePath.resolve(image.getFileName());
        Assert.isTrue(Files.exists(filePath), "The image file does not exist.");

        // Creation date validation
        Assert.notNull(image.getCreationDate(), "The image must have a creation date.");
        boolean isDatePast = image.getCreationDate().isBefore(Instant.now());
        Assert.isTrue(isDatePast, "The image creation date cannot be set in the future.");

        return super.save(image);
    }
 
    @Override
    public AstroImage update(AstroImage image) {
        Assert.notNull(image, "The image cannot be null.");

        // Filename validation
        Assert.hasText(image.getFileName(), "The image must have a file name.");
        String validFileNameRegex = "[^a-zA-Z0-9\\._\\-]";
        Assert.isTrue(!image.getFileName().matches(validFileNameRegex), "The image file name contains illegal characters.");

        Path imgStorePath = observatoryService.getConfig().getCamera().getImageStorePath();
        Path filePath = imgStorePath.resolve(image.getFileName());
        Assert.isTrue(Files.exists(filePath), "The image file does not exist.");

        // Creation date validation
        Assert.notNull(image.getCreationDate(), "The image must have a creation date.");
        boolean isDatePast = image.getCreationDate().isBefore(Instant.now());
        Assert.isTrue(isDatePast, "The image creation date cannot be set in the future.");

        return super.update(image);
    }

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Finds all {@link AstroImage} made by the given user.
     * 
     * @param user The {@link AltairUser} to find images for.
     * 
     * @return A {@link Collection} of images made by the given user.
     */
    public Collection<AstroImage> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");
        return astroImageRepository.findByUserId(user.getId());
    }

    /**
     * Finds all {@link AstroImage} made by the given user in the given
     * {@link Interval} of the given {@link AstroObject}.
     * 
     * @param user The {@link AltairUser} to find images for.
     * @param target The {@link AstroObject} to find images for.
     * @param interval The {@link Interval} to find images in.
     * 
     * @return A {@link List} of images made by the given user in the given
     *         {@link Interval} of the given {@link AstroObject}, ordered by
     *         creation date.
     */
    public List<AstroImage> findByUserAndIntervalAndTarget(AltairUser user, AstroObject target, Interval interval) {
        Assert.notNull(user, "The user cannot be null.");
        Assert.notNull(interval, "The interval cannot be null.");
        Assert.notNull(target, "The target cannot be null.");

        return astroImageRepository.findByCreationDateBetweenAndUserIdAndTargetIdOrderByCreationDateAsc(interval.getStart(), interval.getEnd(), user.getId(), target.getId());
    }

    /**
     * Finds all {@link AstroImage} of the given {@link AstroObject}.
     * 
     * @param target The {@link AstroObject} to find images for.
     * 
     * @return A {@link Collection} of images of the given {@link AstroObject}.
     */
    public Collection<AstroImage> findByTarget(AstroObject target) {
        Assert.notNull(target, "The target cannot be null.");
        return astroImageRepository.findByTargetId(target.getId());
    }
    
    /**
     * Finds all {@link AstroImage} of the given {@link AstroObject} made in
     * the given {@link Interval}.
     * 
     * @param target The {@link AstroObject} to find images for.
     * @param interval The {@link Interval} to find images in.
     * 
     * @return A {@link List} of images of the given {@link AstroObject} made in
     *         the given {@link Interval}, ordered by creation date.
     */
    public List<AstroImage> findByTargetAndInterval(AstroObject target, Interval interval) {
        Assert.notNull(target, "The target cannot be null.");
        Assert.notNull(interval, "The interval cannot be null.");

        return astroImageRepository.findByCreationDateBetweenAndTargetIdOrderByCreationDateAsc(interval.getStart(), interval.getEnd(), target.getId());
    }

    /**
     * Finds all {@link AstroImage} that have the given {@link ImageAttribute}
     * with the given {@link ImageValue}.
     * 
     * @param attr The {@link ImageAttribute} to find images for.
     * @param val  The {@link ImageValue} to find images for. If the value is
     *             {@code null}, only images missing the given attribute will be
     *             returned.
     * 
     * @return A {@link Collection} of images that have the given attribute with
     *         the given value.
     */
    public Collection<AstroImage> findByAttributeValue(ImageAttribute attr, ImageValue val) {
        if (val == null)
            return findByAttributeValue(attr, "");

        Assert.hasText(val.getValue(), "The value cannot be null or empty.");
        return findByAttributeValue(attr, val.getValue());
    }

    /**
     * Finds all {@link AstroImage} that have the given {@link ImageAttribute}
     * with the given value.
     * 
     * @param attr The {@link ImageAttribute} to find images for.
     * @param val  The value to find images for. If the value is {@code null} or
     *             empty, only images missing the given attribute will be returned.
     * 
     * @return A {@link Collection} of images that have the given attribute with
     *         the given value.
     */
    public Collection<AstroImage> findByAttributeValue(ImageAttribute attr, String val) {
        Assert.notNull(attr, "The attribute cannot be null.");
        Assert.hasText(attr.getName(), "The attribute name cannot be null or empty.");
        
        boolean valHasText = val != null && !val.isEmpty();

        if (valHasText)
            return astroImageRepository.findByValuesValueAndValuesAttributeName(val, attr.getName());
        else
            return astroImageRepository.findByValuesAttributeNameAndValuesValueIsNull(attr.getName());
    }

    /**
     * Loads a valid {@link Path} to the image file of the given {@link AstroImage}.
     * 
     * @param image The {@link AstroImage} to load the image file for.
     * @return A valid {@link Path} to the image file of the given {@link AstroImage}.
     */
    public Path getImagePath(AstroImage image) {
        Assert.notNull(image, "The image cannot be null.");
        Assert.hasText(image.getFileName(), "The image must have a file name.");

        Path imgStorePath = observatoryService.getConfig().getCamera().getImageStorePath();
        Path filePath = imgStorePath.resolve(image.getFileName());
        Assert.isTrue(Files.exists(filePath), "The image file does not exist.");

        return filePath;
    }

    //#endregion Methods
    
}

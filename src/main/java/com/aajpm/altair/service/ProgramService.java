package com.aajpm.altair.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.Program;
import com.aajpm.altair.repository.ProgramRepository;

@Service
@Transactional
public class ProgramService extends BasicEntityCRUDService<Program> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ProgramRepository programRepository;

    @Override
    protected ProgramRepository getManagedRepository() {
        return programRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ProgramService() {
        super();
    }

    @Override
    public Program create() {
        return new Program();
    }

    public Program create(String name, AstroObject target) {
        Program program = create();
        program.setName(name);
        program.setTarget(target);
        return program;
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public Program save(Program program) {
        Assert.notNull(program, "The program cannot be null.");

        Assert.hasText(program.getName(), "The program name cannot be empty.");
        Assert.notNull(program.isEnabled(), "The program enabled flag must be set.");
        Assert.notNull(program.getTarget(), "The program must have a target.");

        return super.save(program);
    }

    @Override
    public Program update(Program program) {
        Assert.notNull(program, "The program cannot be null.");

        Assert.hasText(program.getName(), "The program name cannot be empty.");
        Assert.notNull(program.isEnabled(), "The program enabled flag must be set.");
        Assert.notNull(program.getTarget(), "The program must have a target.");

        return super.update(program);
    }

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Fetch a {@link Program} by name. The name is case-insensitive.
     * 
     * @param name The name of the program to fetch, case-insensitive.
     * 
     * @return The {@link Program} with the given name, or null if no such
     *         program is found.
     */
    public Program findByName(String name) {
        return programRepository.findByNameIgnoreCase(name);
    }

    /**
     * Find all {@link Program}s that have the given {@link AstroObject} as their
     * designated target.
     * 
     * @param target The {@link AstroObject} to search programs for.
     * @return A {@link Collection} of {@link Program}s that have the given
     *         {@link AstroObject} as their designated target.
     */
    public Collection<Program> findByTarget(AstroObject target) {
        return programRepository.findByTargetId(target.getId());
    }

    /**
     * Find all {@link Program}s that have the given {@link AstroObject} as their
     * designated target and are currently enabled.
     * 
     * @param target The {@link AstroObject} to search programs for.
     * @return A {@link Collection} of {@link Program}s that have the given
     *         {@link AstroObject} as their designated target and are enabled.
     */
    public Collection<Program> findByTargetEnabled(AstroObject target) {
        return programRepository.findByTargetIdAndEnabledTrue(target.getId());
    }

    //#endregion
    
}

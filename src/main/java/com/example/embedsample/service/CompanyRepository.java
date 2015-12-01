/*
 * Copyright (c) 2001-2015 HPD Software Ltd.
 */
package com.example.embedsample.service;

import com.example.embedsample.domain.Company;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.Trim;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import javax.annotation.PostConstruct;
import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 *
 * A simple facade that provides some domain classes. Not a real repository in the
 * Spring Data sends - although ultimately it may become one.
 *
 * com.example.embedsample.CompanyRepository, created on 27/11/2015 16:23 <p>
 * @author Charles
 */
@Component
public class CompanyRepository implements ResourceLoaderAware {
  public static final CellProcessor CP_MANDATORY_STRING = new NotNull(new Trim());
  public static final CellProcessor CP_OPTIONAL_STRING = new Optional(new Trim());
  public static final CellProcessor CP_OPTIONAL_DATE = new ParseDate("yyyy-MM-dd");
  private static Logger LOGGER = LoggerFactory.getLogger(CompanyRepository.class);
  protected String resourceName = "classpath:companies.csv";
  protected Map<String, Company> regKeyToCompany = Maps.newHashMap();
  protected ResourceLoader resourceLoader;


  public CompanyRepository() {

  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @PostConstruct
  public void init() throws IOException {
    loadCompaniesFromCSV(resourceName);
  }

  /**
   * Loads companies from a CSV file
   *
   * @param resourceName
   * @throws IOException
   */
  protected void loadCompaniesFromCSV(String resourceName) throws IOException {
    Resource resource = resourceLoader.getResource(resourceName);

    List<FieldDescription> descriptions = ImmutableList.of(
        new FieldDescription("RegistrationNumber", CP_MANDATORY_STRING),
        new FieldDescription("CompanyName", CP_MANDATORY_STRING),
        new FieldDescription("ActivityCode", CP_OPTIONAL_STRING),
        new FieldDescription("ActivityDescription", CP_OPTIONAL_STRING),
        new FieldDescription("StatusCode", CP_OPTIONAL_STRING),
        new FieldDescription("StatusDescription", CP_OPTIONAL_STRING),
        new FieldDescription("RegistrationDate", CP_OPTIONAL_DATE)
    );

    String[] fieldMapping = Iterables.toArray(Lists.transform(descriptions, new Function<FieldDescription, String>() {
      @Override
      public String apply(FieldDescription input) {
        return Introspector.decapitalize(input.heading);
      }
    }), String.class);

    CellProcessor[] processors = Iterables.toArray(Lists.transform(descriptions, new Function<FieldDescription, CellProcessor>() {
      @Override
      public CellProcessor apply(FieldDescription input) {
        return input.processor;
      }
    }), CellProcessor.class);


    try (CsvBeanReader reader = new CsvBeanReader(
        new InputStreamReader(resource.getInputStream()),
        CsvPreference.STANDARD_PREFERENCE)
    ) {

      /* Read the headers from the first line */
      String[] header = reader.getHeader(true);


      /* Read the companies */
      for (Company company = reader.read(Company.class, fieldMapping, processors);
           company != null;
           company = reader.read(Company.class, fieldMapping, processors)) {
        addCompany(company);
      }

      if (LOGGER.isDebugEnabled()) {
        String asTest = Joiner.on(",").withKeyValueSeparator("=").join(regKeyToCompany);
        LOGGER.debug("loadCompaniesFromCSV - {}", asTest);
      }
    }

  }

  /**
   * Finds the company by the registration number
   * @param registrationNumber
   * @return the company, or null if it couldn't be found
   */
  public Company getCompanyByRegistrationNumber(String registrationNumber) {
    return regKeyToCompany.get(registrationNumber);
  }


  /**
   * Returns all of the companies, in natural order
   * @return
   */
  public Iterable<Company> getAllCompanies() {
    return ImmutableList.copyOf(regKeyToCompany.values());
  }

  /**
   * Adds a company
   * @param company Can't be null
   */
  protected void addCompany(Company company) {
    Preconditions.checkNotNull(company, "company cannot be null");
    regKeyToCompany.put(company.getRegistrationNumber(), company);
  }


  /**
   * Class to simplify mapping of fields in the CSV
   */
  protected static class FieldDescription {
    String heading;
    CellProcessor processor;

    public FieldDescription(String heading) {
      this(heading, null);
    }

    public FieldDescription(String heading, CellProcessor processor) {
      this.heading = heading;
      this.processor = processor;
    }
  }
}

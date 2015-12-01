/*
 * Copyright (c) 2001-2015 HPD Software Ltd.
 */
package com.example.embedsample.controller;

import com.example.embedsample.domain.Company;
import com.example.embedsample.domain.CompanyOrdering;
import com.example.embedsample.service.CompanyRepository;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * com.example.embedsample.controller.CompaniesAPIController, created on 30/11/2015 09:40 <p>
 * @author Charles
 */

@RestController
@RequestMapping("companies")
public class CompaniesAPIController {

  protected CompanyOrdering companyOrdering = new CompanyOrdering();
  protected CompanyRepository companyRepository;

  @Autowired
  public CompaniesAPIController(CompanyRepository companyRepository) {
    this.companyRepository = companyRepository;
  }

  @RequestMapping()
  public Iterable<CompanySearchResult> getCompanies(
      @RequestParam(defaultValue = "+companyName") String sort
  ) {
    ImmutableSortedSet<Company> sortedCompanies = FluentIterable
        .from(companyRepository.getAllCompanies())
        .toSortedSet(companyOrdering.getOrdering(sort));

    return ImmutableSet.copyOf(Iterables.transform(sortedCompanies, new Function<Company, CompanySearchResult>() {
      @Override
      public CompanySearchResult apply(Company input) {
        return new CompanySearchResult(input.getRegistrationNumber(), input.getCompanyName());
      }
    }));
  }


  @RequestMapping("/{regKey}")
  public Company getCompany(
      @PathVariable("regKey") String regKey
  ) {
    Company company = companyRepository.getCompanyByRegistrationNumber(regKey);
    if (company == null) {
      throw new CompanyNotFoundException();
    }
    return company;
  }


  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Company Not Found")
  protected static class CompanyNotFoundException extends RuntimeException {
  }

}

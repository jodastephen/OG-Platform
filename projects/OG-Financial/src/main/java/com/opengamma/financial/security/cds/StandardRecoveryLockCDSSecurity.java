/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.security.cds;

import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.credit.DebtSeniority;
import com.opengamma.analytics.financial.credit.RestructuringClause;
import com.opengamma.financial.convention.StubType;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.frequency.Frequency;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.swap.InterestRateNotional;
import com.opengamma.id.ExternalId;

/**
 *
 */
@BeanDefinition
public class StandardRecoveryLockCDSSecurity  extends StandardCDSSecurity {

  /** Serialization version */
  private static final long serialVersionUID = 1L;

  /**
   * The security type
   */
  public static final String SECURITY_TYPE = "STANDARD_RECOVERY_LOCK_CDS";
  StandardRecoveryLockCDSSecurity() { // for fudge
    super(SECURITY_TYPE);
  }

  public StandardRecoveryLockCDSSecurity(final boolean isBuy, final ExternalId protectionSeller, final ExternalId protectionBuyer, final ExternalId referenceEntity, //CSIGNORE
      final DebtSeniority debtSeniority, final RestructuringClause restructuringClause, final ExternalId regionId, final ZonedDateTime startDate,
      final ZonedDateTime effectiveDate, final ZonedDateTime maturityDate, final StubType stubType, final Frequency couponFrequency, final DayCount dayCount,
      final BusinessDayConvention businessDayConvention, final boolean immAdjustMaturityDate, final boolean adjustEffectiveDate,
      final boolean adjustMaturityDate, final InterestRateNotional notional, final double recoveryRate, final boolean includeAccruedPremium,
      final boolean protectionStart, final double quotedSpread, final InterestRateNotional upfrontAmount) {
    super(isBuy, protectionSeller, protectionBuyer, referenceEntity, debtSeniority, restructuringClause, regionId, startDate, effectiveDate, maturityDate, stubType,
        couponFrequency, dayCount, businessDayConvention, immAdjustMaturityDate, adjustEffectiveDate, adjustMaturityDate, notional, recoveryRate, includeAccruedPremium,
        protectionStart, quotedSpread, upfrontAmount, SECURITY_TYPE);
  }

  @Override
  public final <T> T accept(final FinancialSecurityVisitor<T> visitor) {
    return visitor.visitStandardRecoveryLockCDSSecurity(this);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StandardRecoveryLockCDSSecurity}.
   * @return the meta-bean, not null
   */
  public static StandardRecoveryLockCDSSecurity.Meta meta() {
    return StandardRecoveryLockCDSSecurity.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(StandardRecoveryLockCDSSecurity.Meta.INSTANCE);
  }

  @Override
  public StandardRecoveryLockCDSSecurity.Meta metaBean() {
    return StandardRecoveryLockCDSSecurity.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StandardRecoveryLockCDSSecurity}.
   */
  public static class Meta extends StandardCDSSecurity.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap());

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    public BeanBuilder<? extends StandardRecoveryLockCDSSecurity> builder() {
      return new DirectBeanBuilder<StandardRecoveryLockCDSSecurity>(new StandardRecoveryLockCDSSecurity());
    }

    @Override
    public Class<? extends StandardRecoveryLockCDSSecurity> beanType() {
      return StandardRecoveryLockCDSSecurity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

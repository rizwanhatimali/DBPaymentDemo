package com.db.cb.dpg;

import static com.db.cb.dpg.contract.model.CreateMarketplacePurchaseRequest.PurchaseTypeEnum.REGULAR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.db.cb.dpg.contract.api.MarketplacePurchaseApi;
import com.db.cb.dpg.contract.model.ApiResponseOfMarketplacePaymentInfo;
import com.db.cb.dpg.contract.model.ApiResponseOfMarketplacePurchaseCheckoutInfo;
import com.db.cb.dpg.contract.model.ApiResponseOfMarketplacePurchaseInfo;
import com.db.cb.dpg.contract.model.CardPaymentDetails;
import com.db.cb.dpg.contract.model.CheckoutMarketplacePurchaseRequest;
import com.db.cb.dpg.contract.model.CreateMarketplacePurchaseDistributionRequest;
import com.db.cb.dpg.contract.model.CreateMarketplacePurchaseRequest;
import com.db.cb.dpg.contract.model.MarketplacePaymentInfo;
import com.db.cb.dpg.contract.model.MarketplacePurchaseCheckoutInfo;
import com.db.cb.dpg.contract.model.MarketplacePurchaseInfo;
import com.db.cb.dpg.contract.model.Money;
import com.db.cb.dpg.contract.model.PaymentSessionDetails;
import com.db.cb.dpg.contract.model.PreauthorizeMarketplacePurchaseRequest;
import com.db.cb.dpg.model.AuthorizationRequest;
import com.db.cb.dpg.model.CheckoutResponse;
import com.db.cb.dpg.model.Payment;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DpgService {

	private final MarketplacePurchaseApi marketplacePurchaseApi;
	private final String sellerId;
	private final String marketplaceId;

	public DpgService(final MarketplacePurchaseApi marketplacePurchaseApi,
					  final @Value("${demo-app.seller-id}") String sellerId,
					  final @Value("${demo-app.marketplace-id}") String marketplaceId) {
		this.marketplacePurchaseApi = marketplacePurchaseApi;
		this.sellerId = sellerId;
		this.marketplaceId = marketplaceId;
	}

	public String createPayment(final Payment payment) {
		var totalAmount = new Money()
				.amount(new BigDecimal(payment.getAmount()))
				.currency(payment.getCurrency());
		var commission = new Money()
				.amount(totalAmount.getAmount().divide(BigDecimal.TEN, RoundingMode.HALF_EVEN))
				.currency(totalAmount.getCurrency());
		var request = new CreateMarketplacePurchaseRequest()
				.amount(totalAmount)
				.buyerCountryCode("DE")
				.description("Some description")
				.addDistributionsItem(new CreateMarketplacePurchaseDistributionRequest()
						.amount(new Money()
								.amount(totalAmount.getAmount().subtract(commission.getAmount()))
								.currency(totalAmount.getCurrency()))
						.marketplaceCommission(commission)
						.sellerId(sellerId))
				.marketplaceId(marketplaceId)
				.externalPurchaseId("ExtPurchaseId" + RandomStringUtils.randomAlphanumeric(10))
				.purchaseType(REGULAR);
		return Optional.ofNullable(marketplacePurchaseApi.createMarketplacePurchase(request).getBody())
				.map(ApiResponseOfMarketplacePurchaseInfo::getData)
				.map(MarketplacePurchaseInfo::getId)
				.orElseThrow();
	}

	public CheckoutResponse checkoutPayment(final Payment payment) {
		var request = new CheckoutMarketplacePurchaseRequest()
				.paymentSessionDetails(new PaymentSessionDetails()
						.selectedPaymentInstrumentId(PaymentSessionDetails.SelectedPaymentInstrumentIdEnum.NEW)
						.selectedPaymentMethod(PaymentSessionDetails.SelectedPaymentMethodEnum.CARDPAYMENT)
				);
		return Optional.ofNullable(marketplacePurchaseApi.checkoutMarketplacePurchaseByMarketplacePurchaseId(payment.getDpgPaymentId(), request).getBody())
				.map(ApiResponseOfMarketplacePurchaseCheckoutInfo::getData)
				.map(info -> this.createCheckoutResponse(info, payment))
				.orElseThrow();
	}

	public String preAuthorizePayment(final Payment payment, final AuthorizationRequest authorizationRequest) {
		var request = new PreauthorizeMarketplacePurchaseRequest();
		request.paymentSessionDetails(new CardPaymentDetails()
				.token(authorizationRequest.getToken())
				.owner("test-owner")
				.selectedPaymentMethod(PaymentSessionDetails.SelectedPaymentMethodEnum.CARDPAYMENT)
				.selectedPaymentInstrumentId(PaymentSessionDetails.SelectedPaymentInstrumentIdEnum.NEW)
		);
		return Optional.ofNullable(marketplacePurchaseApi.preauthorizeMarketplacePurchaseByMarketplacePurchaseId(payment.getDpgPaymentId(), request).getBody())
				.map(ApiResponseOfMarketplacePaymentInfo::getData)
				.map(MarketplacePaymentInfo::getPaymentTransactionId)
				.orElseThrow();
	}

	public String capturePayment(final Payment payment) {
		return Optional.ofNullable(marketplacePurchaseApi.captureMarketplacePurchaseByMarketplacePurchaseId(payment.getDpgPaymentId()).getBody())
				.map(ApiResponseOfMarketplacePaymentInfo::getData)
				.map(MarketplacePaymentInfo::getPaymentTransactionId)
				.orElseThrow();
	}

	private CheckoutResponse createCheckoutResponse(final MarketplacePurchaseCheckoutInfo checkoutInfo,
													final Payment payment) {
		CheckoutResponse checkoutResponse = new CheckoutResponse();
		var requestParameters = checkoutInfo.getRedirectRequired().getRequestParameters();
		checkoutResponse.setPaymentId(payment.getPaymentId());
		checkoutResponse.setSessionId(requestParameters.getClientSession());
		checkoutResponse.setSessionConfig(requestParameters.getClientConfiguration());
		return checkoutResponse;
	}
}

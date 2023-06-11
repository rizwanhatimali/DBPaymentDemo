package com.db.cb.dpg;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
class WebPageController {

	@GetMapping("/")
	public String checkoutPage() {
		return "checkout.html";
	}

	@GetMapping("/payment-status.html")
	public String paymentStatus(
			final @RequestParam String status,
			final @RequestParam String paymentId,
			final Model model
	) {
		model.addAttribute("paymentId", paymentId);
		model.addAttribute("status", status);
		return "payment-status.html";
	}

}

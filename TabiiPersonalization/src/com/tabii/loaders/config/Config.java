package com.tabii.loaders.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {

	private boolean ad_live_show;
	private boolean ad_show;
	private String ads_url;
	private String basePath;
	private CancelPopup cancel_popup;
	private int communication_permission_id;
	private Cookies cookies;
	private CorporateExpiredPackage corporate_expired_package;
	private CorporateLogo corporate_logo;
	private String customer_support_endpoint;
	private CwWriterPath cw_writer_path;
	private int cw_writer_update_frequency;
	private int delete_account_id;
	private boolean discover;
	private int distance_selling_agreement_id;
	private int distance_selling_agreement_param;
	private DowngradePopup downgrade_popup;
	private int entitlement_upgrade_id;
	private String fairPlayCertificateUrl;
	private String file_cdn_path;
	private int geoblock_faq_id;
	private GetmeFrequency getme_frequency;
	private String guest_max_quality;
	private String help_center_endpoint;
	private int help_page_id;
	private String imadai_token_url;
	private String image_cdn_path;
	private String insider_proxy_endpoint;
	private int kids_menu_id;
	private int kids_search_default_queue;
	private int kids_search_failed_queue;
	private int live_stream_queue;
	private int loby_page_id;
	private int menu_id;
	private int preliminary_information_form_id;
	private int preliminary_information_form_param;
	private int privacy_policy_agreement_id;
	private ProdCorpLobby prod_corp_lobby;
	private int row_limit;
	private int search_default_queue;
	private int search_failed_queue;
	private boolean shuffle;
	private int user_agreement_id;
	private String watching_device;
	private int welcome_faq_id;
	private int welcome_page_id;

	@JsonProperty("isAvailable")
	private boolean isAvailable;

	private String region;
	private String country;

	public boolean isAd_live_show() {
		return ad_live_show;
	}

	public void setAd_live_show(boolean ad_live_show) {
		this.ad_live_show = ad_live_show;
	}

	public boolean isAd_show() {
		return ad_show;
	}

	public void setAd_show(boolean ad_show) {
		this.ad_show = ad_show;
	}

	public String getAds_url() {
		return ads_url;
	}

	public void setAds_url(String ads_url) {
		this.ads_url = ads_url;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public CancelPopup getCancel_popup() {
		return cancel_popup;
	}

	public void setCancel_popup(CancelPopup cancel_popup) {
		this.cancel_popup = cancel_popup;
	}

	public int getCommunication_permission_id() {
		return communication_permission_id;
	}

	public void setCommunication_permission_id(int communication_permission_id) {
		this.communication_permission_id = communication_permission_id;
	}

	public Cookies getCookies() {
		return cookies;
	}

	public void setCookies(Cookies cookies) {
		this.cookies = cookies;
	}

	public CorporateExpiredPackage getCorporate_expired_package() {
		return corporate_expired_package;
	}

	public void setCorporate_expired_package(CorporateExpiredPackage corporate_expired_package) {
		this.corporate_expired_package = corporate_expired_package;
	}

	public CorporateLogo getCorporate_logo() {
		return corporate_logo;
	}

	public void setCorporate_logo(CorporateLogo corporate_logo) {
		this.corporate_logo = corporate_logo;
	}

	public String getCustomer_support_endpoint() {
		return customer_support_endpoint;
	}

	public void setCustomer_support_endpoint(String customer_support_endpoint) {
		this.customer_support_endpoint = customer_support_endpoint;
	}

	public CwWriterPath getCw_writer_path() {
		return cw_writer_path;
	}

	public void setCw_writer_path(CwWriterPath cw_writer_path) {
		this.cw_writer_path = cw_writer_path;
	}

	public int getCw_writer_update_frequency() {
		return cw_writer_update_frequency;
	}

	public void setCw_writer_update_frequency(int cw_writer_update_frequency) {
		this.cw_writer_update_frequency = cw_writer_update_frequency;
	}

	public int getDelete_account_id() {
		return delete_account_id;
	}

	public void setDelete_account_id(int delete_account_id) {
		this.delete_account_id = delete_account_id;
	}

	public boolean isDiscover() {
		return discover;
	}

	public void setDiscover(boolean discover) {
		this.discover = discover;
	}

	public int getDistance_selling_agreement_id() {
		return distance_selling_agreement_id;
	}

	public void setDistance_selling_agreement_id(int distance_selling_agreement_id) {
		this.distance_selling_agreement_id = distance_selling_agreement_id;
	}

	public int getDistance_selling_agreement_param() {
		return distance_selling_agreement_param;
	}

	public void setDistance_selling_agreement_param(int distance_selling_agreement_param) {
		this.distance_selling_agreement_param = distance_selling_agreement_param;
	}

	public DowngradePopup getDowngrade_popup() {
		return downgrade_popup;
	}

	public void setDowngrade_popup(DowngradePopup downgrade_popup) {
		this.downgrade_popup = downgrade_popup;
	}

	public int getEntitlement_upgrade_id() {
		return entitlement_upgrade_id;
	}

	public void setEntitlement_upgrade_id(int entitlement_upgrade_id) {
		this.entitlement_upgrade_id = entitlement_upgrade_id;
	}

	public String getFairPlayCertificateUrl() {
		return fairPlayCertificateUrl;
	}

	public void setFairPlayCertificateUrl(String fairPlayCertificateUrl) {
		this.fairPlayCertificateUrl = fairPlayCertificateUrl;
	}

	public String getFile_cdn_path() {
		return file_cdn_path;
	}

	public void setFile_cdn_path(String file_cdn_path) {
		this.file_cdn_path = file_cdn_path;
	}

	public int getGeoblock_faq_id() {
		return geoblock_faq_id;
	}

	public void setGeoblock_faq_id(int geoblock_faq_id) {
		this.geoblock_faq_id = geoblock_faq_id;
	}

	public GetmeFrequency getGetme_frequency() {
		return getme_frequency;
	}

	public void setGetme_frequency(GetmeFrequency getme_frequency) {
		this.getme_frequency = getme_frequency;
	}

	public String getGuest_max_quality() {
		return guest_max_quality;
	}

	public void setGuest_max_quality(String guest_max_quality) {
		this.guest_max_quality = guest_max_quality;
	}

	public String getHelp_center_endpoint() {
		return help_center_endpoint;
	}

	public void setHelp_center_endpoint(String help_center_endpoint) {
		this.help_center_endpoint = help_center_endpoint;
	}

	public int getHelp_page_id() {
		return help_page_id;
	}

	public void setHelp_page_id(int help_page_id) {
		this.help_page_id = help_page_id;
	}

	public String getImadai_token_url() {
		return imadai_token_url;
	}

	public void setImadai_token_url(String imadai_token_url) {
		this.imadai_token_url = imadai_token_url;
	}

	public String getImage_cdn_path() {
		return image_cdn_path;
	}

	public void setImage_cdn_path(String image_cdn_path) {
		this.image_cdn_path = image_cdn_path;
	}

	public String getInsider_proxy_endpoint() {
		return insider_proxy_endpoint;
	}

	public void setInsider_proxy_endpoint(String insider_proxy_endpoint) {
		this.insider_proxy_endpoint = insider_proxy_endpoint;
	}

	public int getKids_menu_id() {
		return kids_menu_id;
	}

	public void setKids_menu_id(int kids_menu_id) {
		this.kids_menu_id = kids_menu_id;
	}

	public int getKids_search_default_queue() {
		return kids_search_default_queue;
	}

	public void setKids_search_default_queue(int kids_search_default_queue) {
		this.kids_search_default_queue = kids_search_default_queue;
	}

	public int getKids_search_failed_queue() {
		return kids_search_failed_queue;
	}

	public void setKids_search_failed_queue(int kids_search_failed_queue) {
		this.kids_search_failed_queue = kids_search_failed_queue;
	}

	public int getLive_stream_queue() {
		return live_stream_queue;
	}

	public void setLive_stream_queue(int live_stream_queue) {
		this.live_stream_queue = live_stream_queue;
	}

	public int getLoby_page_id() {
		return loby_page_id;
	}

	public void setLoby_page_id(int loby_page_id) {
		this.loby_page_id = loby_page_id;
	}

	public int getMenu_id() {
		return menu_id;
	}

	public void setMenu_id(int menu_id) {
		this.menu_id = menu_id;
	}

	public int getPreliminary_information_form_id() {
		return preliminary_information_form_id;
	}

	public void setPreliminary_information_form_id(int preliminary_information_form_id) {
		this.preliminary_information_form_id = preliminary_information_form_id;
	}

	public int getPreliminary_information_form_param() {
		return preliminary_information_form_param;
	}

	public void setPreliminary_information_form_param(int preliminary_information_form_param) {
		this.preliminary_information_form_param = preliminary_information_form_param;
	}

	public int getPrivacy_policy_agreement_id() {
		return privacy_policy_agreement_id;
	}

	public void setPrivacy_policy_agreement_id(int privacy_policy_agreement_id) {
		this.privacy_policy_agreement_id = privacy_policy_agreement_id;
	}

	public ProdCorpLobby getProd_corp_lobby() {
		return prod_corp_lobby;
	}

	public void setProd_corp_lobby(ProdCorpLobby prod_corp_lobby) {
		this.prod_corp_lobby = prod_corp_lobby;
	}

	public int getRow_limit() {
		return row_limit;
	}

	public void setRow_limit(int row_limit) {
		this.row_limit = row_limit;
	}

	public int getSearch_default_queue() {
		return search_default_queue;
	}

	public void setSearch_default_queue(int search_default_queue) {
		this.search_default_queue = search_default_queue;
	}

	public int getSearch_failed_queue() {
		return search_failed_queue;
	}

	public void setSearch_failed_queue(int search_failed_queue) {
		this.search_failed_queue = search_failed_queue;
	}

	public boolean isShuffle() {
		return shuffle;
	}

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}

	public int getUser_agreement_id() {
		return user_agreement_id;
	}

	public void setUser_agreement_id(int user_agreement_id) {
		this.user_agreement_id = user_agreement_id;
	}

	public String getWatching_device() {
		return watching_device;
	}

	public void setWatching_device(String watching_device) {
		this.watching_device = watching_device;
	}

	public int getWelcome_faq_id() {
		return welcome_faq_id;
	}

	public void setWelcome_faq_id(int welcome_faq_id) {
		this.welcome_faq_id = welcome_faq_id;
	}

	public int getWelcome_page_id() {
		return welcome_page_id;
	}

	public void setWelcome_page_id(int welcome_page_id) {
		this.welcome_page_id = welcome_page_id;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

}

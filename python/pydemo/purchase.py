
import os

from flask import (Blueprint, current_app, flash, json,
                   redirect, render_template, request, session, url_for)

bp = Blueprint('purchase', __name__, url_prefix='/')


@bp.context_processor
def appinfo():
    return {'appname': current_app.name}


@bp.route('/', methods=['GET', 'POST'])
def start():
    import time

    try:
        with open(os.path.join(current_app.root_path, 'data', 'create.json'), 'r') as file:
            payload = file.read()
        payload = payload.replace(
            '{{marketplaceId}}', current_app.config['MARKETPLACE_ID'])
        payload = payload.replace(
            '{{marketplaceSellerId}}', current_app.config['MARKETPLACE_SELLER_ID'])
        if request.method == 'POST':
            external_purchase_id = session['externalPurchaseId']
            payload = payload.replace(
                '{{newExternalPurchaseId}}', external_purchase_id)
            target_url = '/services/digital/dpg/sbx/purchase/v1/marketplace/purchases'
            purchase_id = dpg_post(target_url, payload)['data']['id']
            session['purchaseId'] = purchase_id
            with open(os.path.join(current_app.root_path, 'data', 'checkout.json'), 'r') as file:
                payload = file.read()
            target_url = f'/services/digital/dpg/sbx/purchase/v1/marketplace/purchases/{purchase_id}:checkout'
            request_parameters = dpg_post(target_url, payload)[
                'data']['redirectRequired']['requestParameters']
            session['requestParameters'] = request_parameters
            return redirect(url_for('purchase.proceed'))
    except Exception as ex:
        flash(str(ex), category='failure')
    external_purchase_id = 'extprchid' + str(int(time.time() * 1000))
    session['externalPurchaseId'] = external_purchase_id
    payload = payload.replace(
        '{{newExternalPurchaseId}}', external_purchase_id)
    flash('We have prepared a new purchase as follows for you. Press "Proceed" to continue with checkout.', category='success')
    flash(payload, category='info')
    return render_template('/start.html')


@bp.route('/proceed', methods=['GET', 'POST'])
def proceed():
    try:
        purchase_id = session['purchaseId']
        if request.method == 'POST':
            token = request.form['cardToken']
            with open(os.path.join(current_app.root_path, 'data', 'preauthorize.json'), 'r') as file:
                payload = file.read()
            payload = payload.replace('{{token}}', token)
            target_url = f'/services/digital/dpg/sbx/purchase/v1/marketplace/purchases/{purchase_id}:preauthorize'
            dpg_post(target_url, payload)
            session['state'] = 'preauthorized'
            if 'capture' in request.form:
                with open(os.path.join(current_app.root_path, 'data', 'capture.json'), 'r') as file:
                    payload = file.read()
                target_url = f'/services/digital/dpg/sbx/purchase/v1/marketplace/purchases/{purchase_id}:capture'
                dpg_post(target_url, payload)
                session['state'] = 'captured'
            return redirect(url_for('purchase.finish'))
    except Exception as ex:
        flash(str(ex), category='failure')
    flash('Your purchase was successfully created and checked out. Press "Finish" to complete the purchase flow.', category='success')
    flash('4111111111111111 [   Visa   ]', category='info')
    flash('5232050000010003 [Mastercard]', category='info')
    return render_template('/proceed.html')


@bp.route('/finish', methods=['GET', 'POST'])
def finish():
    if request.method == 'POST':
        return redirect(url_for('purchase.start'))
    state = session['state']
    flash('Purchase flow is finished now.', category='success')
    flash(f'Flow state is "{state}".', category='info')
    return render_template('/finish.html')


def dpg_post(target_url: str, payload: str):
    import base64
    import datetime
    import hashlib

    from OpenSSL import crypto
    from requests import Session
    from requests_pkcs12 import Pkcs12Adapter

    api_key = current_app.config['API_KEY']
    now = datetime.datetime.now(datetime.timezone.utc).strftime(
        '%a, %d %b %Y %H:%M:%S GMT')
    digest = 'SHA-256={}'.format(
        base64.b64encode(hashlib.sha256(payload.encode('utf-8')).digest()).decode('utf-8'))
    header_data = f'(request-target): post {target_url}\ndate: {now}\ndigest: {digest}'
    cs_keystore_filename = os.path.join(
        current_app.root_path, current_app.config['CS_KEYSTORE_FILENAME'])
    cs_keystore_password = current_app.config['CS_KEYSTORE_PASSWORD']
    with open(cs_keystore_filename, 'rb') as file:
        private_key = crypto.load_pkcs12(
            file.read(), passphrase=cs_keystore_password).get_privatekey()
    signed_header_data = base64.b64encode(crypto.sign(
        private_key, header_data, 'sha256')).decode('utf-8')
    signed_header_name = '(request-target) date digest'
    key_id = current_app.config['PUBLIC_KEY_ID']
    signature = f'keyId="{key_id}",algorithm="rsa-sha256",headers="{signed_header_name}",signature="{signed_header_data}"'
    headers = {
        'Api-Key': f'{api_key}',
        'Accept': '*/*',
        'Content-type': 'application/json',
        'Date': f'{now}',
        'Digest': f'{digest}',
        'Signature': f'{signature}'
    }
    with Session() as s:
        tls_keystore_filename = os.path.join(
            current_app.root_path, current_app.config['TLS_KEYSTORE_FILENAME'])
        tls_keystore_password = current_app.config['TLS_KEYSTORE_PASSWORD']
        s.mount(current_app.config['BASE_URL'], Pkcs12Adapter(
            pkcs12_filename=tls_keystore_filename, pkcs12_password=tls_keystore_password))
        response = s.post(
            url=current_app.config['BASE_URL'] + target_url, data=payload, headers=headers)
        if not (response.status_code in (200, 201)):
            error_text = response.text
            if 'Content-Type' in response.headers and response.headers['Content-Type'].find('application/json') != -1:
                error_text = json.dumps(json.loads(error_text), indent=2)
            raise Exception(
                f'Call to the DPG API failed 😱\nEndpoint: {target_url}\nHTTP status code: {response.status_code}\nResponse:\n{error_text}')
        return response.json()

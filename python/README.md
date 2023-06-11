# Demo Pack
This is a simple Flask application developed to demonstrate how to work with our API. 

## How to setup
Please make sure following Python modules are installed:
- `flask`
- `colorama`
- `requests`
- `requests-pkcs12`

For example, if you are using `conda` virtual environments, you can set it with:

`>conda create --prefix .venv flask colorama requests`
`>conda install --prefix .venv -c conda-forge requests_pkcs12`

To use a Python 3 virtual environment, run:

`>python3 -m venv .venv`
`>.venv/bin/pip install --upgrade pip`
`>.venv/bin/pip install -r requirements.txt`
`>source .venv/bin/activate`

## How to run

Make sure that proxy is confugured if needed (e.g. through `https_proxy` environment variable),
and set flask environment variables:
- `FLASK_APP=pydemo`
- `FLASK_ENV=development`

In the project directory run
`python -m flask run --no-debugger --host=0.0.0.0 --port=9090` -
it will launch the application on http://localhost:9090 

## How to configure
All configuration settings are located here: `/pydemo/config.py`
certificate is here: `../secret/dpg-demo-pack.dpg.db.com.p12`
and certificate password is here: `../secret/dpg-demo-pack.dpg.db.com.txt`

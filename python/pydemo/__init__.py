from flask import Flask


def create_app(config_file=None):
    # create and configure the app
    app = Flask(__name__, instance_relative_config=False)
    app.config.from_mapping(
        SECRET_KEY='dev',
    )

    if config_file is None:
        # load default instance config
        app.config.from_pyfile('config.py', silent=False)
    else:
        # load custom config, if passed in
        app.config.from_mapping(config_file)

    # register purchase blueprint
    from . import purchase
    app.register_blueprint(purchase.bp)

    # a simple page that says about the sample
    @app.route('/about')
    def index():
        return 'This is a Marketplace Demo pack'

    return app

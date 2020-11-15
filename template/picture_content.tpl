{combine_script id='pannellum' path=$PIWIGOPANORAMA_PATH|cat:'template/lib/pannellum/pannellum.min.js' load='async' require='jquery'}
{combine_script id='piwigopanorama' path=$PIWIGOPANORAMA_PATH|cat:'template/piwigopanorama.js' load='async' require='pannellum'}

{combine_css id='pannellum' path=$PIWIGOPANORAMA_PATH|cat:'template/lib/pannellum/pannellum.css'}

{$PIWIGOPANORAMA_CONTENT}

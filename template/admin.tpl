<div class="titlePage">
  <h2>Panorama Viewer</h2>
</div>

<form method="post" action="" class="properties" id="pv-config">
<fieldset>
  <legend>{'Configuration'|translate}</legend>
  <ul>
    <li>
      <label>
        <b>{'Auto-load panoramas'|translate}: </b>
        <span class="graphicalCheckbox icon-check{if not $piwigopanorama.auto_load}-empty{/if}"></span>
        <input type="checkbox" name="auto_load"{if $piwigopanorama.auto_load} checked="checked"{/if}>
      </label>
    </li>
    <li>
      <label>
        <b>{'Show zoom controls'|translate}: </b>
        <span class="graphicalCheckbox icon-check{if not $piwigopanorama.show_zoomcntrl}-empty{/if}"></span>
        <input type="checkbox" name="show_zoomcntrl"{if $piwigopanorama.show_zoomcntrl} checked="checked"{/if}>
      </label>
    </li>
    <li>
      <label>
        <b>{'Enable keyboard zoom'|translate}: </b>
        <span class="graphicalCheckbox icon-check{if not $piwigopanorama.enable_keyboard_zoom}-empty{/if}"></span>
        <input type="checkbox" name="enable_keyboard_zoom"{if $piwigopanorama.enable_keyboard_zoom} checked="checked"{/if}>
      </label>
    </li>
    <li>
      <label>
        <b>{'Enable mouse zoom'|translate}: </b>
        <span class="graphicalCheckbox icon-check{if not $piwigopanorama.enable_mouse_zoom}-empty{/if}"></span>
        <input type="checkbox" name="enable_mouse_zoom"{if $piwigopanorama.enable_mouse_zoom} checked="checked"{/if}>
      </label>
    </li>
	<li>
	  <label for="cntrl_location">
	    <b>{'Control location'|@translate}: </b> 
	  <select class="categoryDropDown" id="cntrl_location" name="cntrl_location">
	    {html_options options=$all_control_locations selected=$piwigopanorama.cntrl_location}
	  </select>
	  </label>
	</li>
    <li>
      <label>
        <b>{'Show compass'|translate}: </b>
        <span class="graphicalCheckbox icon-check{if not $piwigopanorama.show_compass}-empty{/if}"></span>
        <input type="checkbox" name="show_compass"{if $piwigopanorama.show_compass} checked="checked"{/if}>
      </label>
    </li>
    <li>
      <label>
        <b>{'Initial field of view'|translate}: </b>
        <input type="number" min="90" max="180" value="{$piwigopanorama.initial_hfov}" name="initial_hfov"> <b>px</b>
      {'Between 90° and 180°.'|translate}
      </label>
    <li>
  </ul>
</fieldset>

<p class="formButtons"><input type="submit" name="save_config" value="{'Save Settings'|translate}"></p>
</form>


{html_style}
.graphicalCheckbox {
  font-size:16px;
  line-height:16px;
}

.graphicalCheckbox + input {
  display:none;
}
{/html_style}

{footer_script}
jQuery('#pv-config input[type=checkbox]').change(function() {
  jQuery(this).prev().toggleClass('icon-check icon-check-empty');
});
jQuery('#pv-config input[type=radio]').change(function() {
  jQuery('#pv-config input[type=radio][name='+ $(this).attr('name') +']').prev().toggleClass('icon-check icon-check-empty');
});
{/footer_script}
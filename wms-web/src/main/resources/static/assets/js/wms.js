(function () {
    // CSRF token on every htmx request
    // document.addEventListener('htmx:configRequest', function (e) {
    //     var token = document.querySelector('meta[name="_csrf"]');
    //     var header = document.querySelector('meta[name="_csrf_header"]');
    //     if (token && header) { e.detail.headers[header.content] = token.content; }
    // });

    document.addEventListener('htmx:configRequest', function (evt) {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header && token.content && header.content) {
            evt.detail.headers[header.content] = token.content;
        } else {
            console.warn('CSRF meta tags missing or empty: htmx POST requests will be rejected');
        }
    });

    // Widgets that need explicit init inside swapped content
    document.addEventListener('htmx:afterSwap', function (e) {
        if (window.jQuery) { jQuery(e.detail.target).find('[data-toggle="tooltip"]').tooltip(); }
        // toasts disappear by themselves
        document.querySelectorAll('.wms-toast:not([data-timer])').forEach(function (el) {
            el.setAttribute('data-timer', '1');
            setTimeout(function () { el.remove(); }, 6000);
        });
    });
})();
/* Campaign links are generated locally; no credentials or tracking service is involved. */
(function () {
    const form = document.getElementById('odp-campaign');
    if (!form) return;
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        const result = document.getElementById('odp-campaign-result');
        const status = document.getElementById('odp-campaign-status');
        try {
            const data = new FormData(form);
            const url = new URL(data.get('url'));
            if (url.origin !== window.location.origin || url.username || url.password) throw new Error('Wpisz adres strony w tym sklepie.');
            for (const key of ['source', 'medium', 'campaign', 'content']) {
                const value = String(data.get(key) || '').trim();
                if (key !== 'content' && !value) throw new Error('Wypełnij źródło, medium i nazwę kampanii.');
                if (value) url.searchParams.set('utm_' + key, value);
                else url.searchParams.delete('utm_' + key);
            }
            result.value = url.toString();
            result.focus();
            result.select();
            status.textContent = 'Link gotowy. Skopiuj go. Na etapie testowym odbiorca musi mieć dostęp do Team VPN.';
        } catch (error) {
            result.value = '';
            status.textContent = error.message;
        }
    });
})();

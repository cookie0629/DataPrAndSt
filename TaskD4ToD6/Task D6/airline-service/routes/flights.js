const express = require('express');
const router = express.Router();

const validator = require('../middlewares/validators/flightsValidator');
const controller = require('../controllers/flights');

router.get('/', validator.isFlightSearchQuery, controller.getFlights);
router.post('/checkins', validator.isCheckinRequest, controller.checkIn);

module.exports = router;

const express = require('express');
const router = express.Router();

const validator = require('../middlewares/validators/flightsValidator');
const controller = require('../controllers/flights');

router.post('/', validator.isCheckinRequest, controller.checkIn);

module.exports = router;
